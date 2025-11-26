

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * SecureChatServer
 *
 * - écoute sur PORT (modifiable) - pour chaque connexion : 1) envoie sa clé
 * publique RSA (X.509 encoded) 2) reçoit la clé AES (chiffrée RSA OAEP) 3)
 * ajoute le client à la liste - quand un client envoie un message, le serveur
 * le déchiffre puis le re-chiffre séparément pour chaque client (broadcast
 * sécurisé)
 *
 * Algorithmes : - RSA/ECB/OAEPWithSHA-256AndMGF1Padding pour l'échange de clé -
 * AES/GCM/NoPadding pour le chiffrement des messages (IV 12 bytes, tag 128
 * bits)
 *
 * Port : 2319 (tu peux le changer)
 */
public class SecureChatServer {

	public static final int PORT = 2319;
	private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
	private final ExecutorService pool = Executors.newCachedThreadPool();

	public static void main(String[] args) throws Exception {
		new SecureChatServer().start();
	}

	public void start() throws Exception {
		ServerSocket serverSocket = new ServerSocket(PORT);
		System.out.println("SecureChatServer démarré sur le port " + PORT);

		while (true) {
			Socket socket = serverSocket.accept();
			System.out.println("Nouvelle connexion : " + socket.getRemoteSocketAddress());
			ClientHandler handler = new ClientHandler(socket, this);
			clients.add(handler);
			pool.submit(handler);
		}
	}

	// broadcast : envoie msgPlain à tous les clients (le msgPlain est en clair côté
	// serveur)
	public void broadcast(String msgPlain, ClientHandler from) {
		synchronized (clients) {
			for (ClientHandler c : clients) {
				try {
					// envoi chiffré séparé avec la clé AES propre à chaque client
					c.sendEncrypted(msgPlain);
				} catch (Exception e) {
					System.err.println("Erreur en broadcast vers " + c.getPeer() + " : " + e.getMessage());
				}
			}
		}
		System.out.println("[BROADCAST] " + msgPlain);
	}

	public void removeClient(ClientHandler c) {
		clients.remove(c);
		System.out.println("Client supprimé: " + c.getPeer());
	}

	// ClientHandler en tant que classe interne
	static class ClientHandler implements Runnable {
		private final Socket socket;
		private final SecureChatServer server;
		private DataInputStream in;
		private DataOutputStream out;

		// après échange RSA->AES
		private SecretKey aesKey;
		private String pseudo;

		// paire RSA du serveur (générée une seule fois pour ce handler)
		private KeyPair rsaPair;

		ClientHandler(Socket socket, SecureChatServer server) {
			this.socket = socket;
			this.server = server;
			this.pseudo = "User-" + socket.getPort();
		}

		public String getPeer() {
			return socket.getRemoteSocketAddress().toString();
		}

		@Override
		public void run() {
			try {
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());

				// 1) Générer paire RSA (server-side ephemeral pour cette connexion)
				KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
				kpg.initialize(2048);
				rsaPair = kpg.generateKeyPair();

				// 2) Envoyer clé publique RSA au client
				byte[] pub = rsaPair.getPublic().getEncoded();
				out.writeInt(pub.length);
				out.write(pub);
				out.flush();

				// 3) Recevoir clé AES chiffrée (RSA OAEP)
				int encLen = in.readInt();
				byte[] encAes = new byte[encLen];
				in.readFully(encAes);

				Cipher rsaDec = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
				rsaDec.init(Cipher.DECRYPT_MODE, rsaPair.getPrivate());
				byte[] aesRaw = rsaDec.doFinal(encAes);
				aesKey = new SecretKeySpec(aesRaw, "AES");

				System.out.println("Clé AES reçue et déchiffrée pour " + getPeer());

				// Annoncer arrivée
				server.broadcast("[SYSTEM] " + pseudo + " a rejoint le chat", this);

				// 4) Boucle de lecture des messages chiffrés venant de ce client
				while (true) {
					// framing : ivLen | iv | ctLen | ct
					int ivLen = in.readInt();
					if (ivLen <= 0)
						break;
					byte[] iv = new byte[ivLen];
					in.readFully(iv);

					int ctLen = in.readInt();
					byte[] ct = new byte[ctLen];
					in.readFully(ct);

					String plain = decryptAesGcm(aesKey, iv, ct);

					// gestion des commandes simples (/nick, bye)
					if (plain.startsWith("/nick ")) {
						String newNick = plain.substring(6).trim();
						String old = pseudo;
						pseudo = newNick.isEmpty() ? pseudo : newNick;
						server.broadcast("[SYSTEM] " + old + " est maintenant " + pseudo, this);
						continue;
					}

					if ("bye".equalsIgnoreCase(plain.trim())) {
						server.broadcast("[SYSTEM] " + pseudo + " a quitté le chat", this);
						break;
					}

					String toBroadcast = pseudo + ": " + plain;
					server.broadcast(toBroadcast, this);
				}

			} catch (EOFException eof) {
				System.out.println("Connexion terminée par le client " + getPeer());
			} catch (Exception e) {
				System.err.println("Erreur ClientHandler (" + getPeer() + ") : " + e.getMessage());
				e.printStackTrace();
			} finally {
				try {
					socket.close();
				} catch (IOException ignored) {
				}
				server.removeClient(this);
			}
		}

		// envoie msgPlain au client (chiffré avec sa clé AES)
		public void sendEncrypted(String msgPlain) throws Exception {
			if (aesKey == null)
				return; // pas encore initialisé
			byte[] plain = msgPlain.getBytes("UTF-8");

			SecureRandom rnd = SecureRandom.getInstanceStrong();
			byte[] iv = new byte[12];
			rnd.nextBytes(iv);

			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec spec = new GCMParameterSpec(128, iv);
			c.init(Cipher.ENCRYPT_MODE, aesKey, spec);
			byte[] ct = c.doFinal(plain);

			synchronized (out) {
				out.writeInt(iv.length);
				out.write(iv);
				out.writeInt(ct.length);
				out.write(ct);
				out.flush();
			}

			// logs (base64) pour debugging seulement
			System.out.println("-> Envoi chiffré à " + getPeer() + " : " + Base64.getEncoder().encodeToString(ct));
		}

		private String decryptAesGcm(SecretKey key, byte[] iv, byte[] ct) throws Exception {
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec spec = new GCMParameterSpec(128, iv);
			c.init(Cipher.DECRYPT_MODE, key, spec);
			byte[] plain = c.doFinal(ct);
			return new String(plain, "UTF-8");
		}
	}
}
