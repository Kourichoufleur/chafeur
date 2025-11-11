import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Serveur {
	public static void main(String[] args) {
		int port = PARAMETRE.port + 1;
		System.out.println("Serveur en attente sur le port " + port + "...");
		SecretKey AES_key;
		Cipher cipher = null;
		try {
			AES_key = echanger_AES();
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, AES_key);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}

		try (ServerSocket serveur = new ServerSocket(port)) {
			Socket clientSocket = serveur.accept();
			System.out.println("Client connecté !");
			DataInputStream in = new DataInputStream(clientSocket.getInputStream());

			int messageSize;
			String message = null;
			while ((messageSize = in.readInt()) != 0) {
				byte[] encrypte = new byte[messageSize];
				in.readFully(encrypte);
				try {
					message = new String(cipher.doFinal(encrypte));
				} catch (IllegalBlockSizeException | BadPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (message.equalsIgnoreCase("bye")) {
					System.out.println("Fermeture demandée par le client...");
					break;
				}
				System.out.println("Message reçu : " + message);
			}
			System.out.println("Client déconnecté.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static SecretKey echanger_AES() throws IOException, NoSuchAlgorithmException, ClassNotFoundException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		int port = PARAMETRE.port;
		ServerSocket serveur = new ServerSocket(port);
		Socket clientSocket = serveur.accept();
		ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();

		out.writeObject(keyPair.getPublic());
		out.flush();
		byte[] aesChiffree = (byte[]) in.readObject();
		Cipher cipherRSA = Cipher.getInstance("RSA");
		cipherRSA.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		byte[] aesBytes = cipherRSA.doFinal(aesChiffree);

		SecretKey aesKey = new SecretKeySpec(aesBytes, "AES");
		System.out.println("Clé AES reçue et déchiffrée !");

		return aesKey;
	}

}