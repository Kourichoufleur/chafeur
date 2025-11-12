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
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Serveur {
	
	private ArrayList<ClientRegistration> clients_enregistres = new ArrayList<ClientRegistration>();
	private ServerSocket serveur_socket;
	
	public Serveur() throws IOException {
		ServerSocket serveur_socket = new ServerSocket(PARAMETRE.port);
	}
	
	// Pour l'instant le main est un echange basique client/serveur
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



	
	
	/**
	 *  Demande aux clients ses informations  tout en conservant la connexion
	 *  et en gardant également la connexion
	 * @param serveur
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public static ClientRegistration registerClient(ServerSocket serveur)
	        throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
	        NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

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

	    System.out.println("Client connecté, clé AES échangée !");
	    return new ClientRegistration(clientSocket, in, out, aesKey);
	}
	
	/**
	 * Permet de créer une connexion client/serveur entre deux clients, et par la même occasion d'échanger leurs clés
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */
	public void echanger_AES_entre_client() throws InvalidKeyException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		    int port = PARAMETRE.port;
		    
		    assert this.serveur_socket != null;


		    System.out.println("En attente du premier client...");
		    ClientRegistration client1 = registerClient(this.serveur_socket);
		    this.clients_enregistres.add(client1);

		    System.out.println("En attente du second client...");
		    ClientRegistration client2 = registerClient(this.serveur_socket);
		    this.clients_enregistres.add(client2);
		    
		    // J'ai mtn les deux clés des deux clients
		    
		}

}
	
