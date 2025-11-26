

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * SecureChatClient
 *
 * - se connecte au serveur (localhost:2319) - reçoit la clé publique RSA -
 * génère une clé AES-256 et l'envoie chiffrée RSA-OAEP au serveur - ensuite :
 * lit clavier → chiffre AES-GCM → envoie au serveur (framed) - et écoute un
 * thread qui reçoit iv+ct, déchiffre et affiche
 *
 * Commandes : /nick NOM -> change ton pseudo bye -> quitte le chat
 */
public class SecureChatClient {

	public static final String HOST = "localhost";
	public static final int PORT = 2319;

	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	private SecretKey aesKey;

	public static void main(String[] args) throws Exception {
		new SecureChatClient().start();
	}

	public void start() throws Exception {
		socket = new Socket(HOST, PORT);
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		System.out.println("Connecté au serveur " + socket.getRemoteSocketAddress());

		// 1) recevoir clé publique RSA
		int pubLen = in.readInt();
		byte[] pubBytes = new byte[pubLen];
		in.readFully(pubBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec ks = new X509EncodedKeySpec(pubBytes);
		PublicKey serverPub = kf.generatePublic(ks);

		// 2) générer AES-256 et l'envoyer chiffrée par RSA-OAEP
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(256);
		aesKey = kg.generateKey();
		byte[] aesRaw = aesKey.getEncoded();

		Cipher rsaEnc = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		rsaEnc.init(Cipher.ENCRYPT_MODE, serverPub);
		byte[] encAes = rsaEnc.doFinal(aesRaw);

		out.writeInt(encAes.length);
		out.write(encAes);
		out.flush();

		System.out.println("Échange de clé AES effectué (AES-256).");

		// thread écouteur
		new Thread(this::listenLoop).start();

		// lecture clavier
		Scanner sc = new Scanner(System.in);
		while (true) {
			String line = sc.nextLine();
			sendEncrypted(line);
			if ("bye".equalsIgnoreCase(line.trim()))
				break;
		}

		socket.close();
		sc.close();
	}

	// envoi d'un message : iv + ct (longs encodés en binaire)
	private void sendEncrypted(String msg) throws Exception {
		byte[] plain = msg.getBytes("UTF-8");

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

		System.out.println("[Envoi] Clair: " + msg);
		System.out.println("[Envoi] Chiffré (base64): " + Base64.getEncoder().encodeToString(ct));
	}

	// écoute des messages du serveur
	private void listenLoop() {
		try {
			while (true) {
				int ivLen = in.readInt();
				if (ivLen <= 0)
					break;
				byte[] iv = new byte[ivLen];
				in.readFully(iv);

				int ctLen = in.readInt();
				byte[] ct = new byte[ctLen];
				in.readFully(ct);

				String plain = decryptAesGcm(aesKey, iv, ct);
				System.out.println("[Serveur] " + plain);
				System.out.println("[Serveur] Chiffré (b64): " + Base64.getEncoder().encodeToString(ct));
			}
		} catch (EOFException eof) {
			System.out.println("Serveur fermé.");
		} catch (Exception e) {
			System.err.println("Erreur écoute: " + e.getMessage());
		} finally {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	private String decryptAesGcm(SecretKey key, byte[] iv, byte[] ct) throws Exception {
		Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(128, iv);
		c.init(Cipher.DECRYPT_MODE, key, spec);
		byte[] plain = c.doFinal(ct);
		return new String(plain, "UTF-8");
	}
}
