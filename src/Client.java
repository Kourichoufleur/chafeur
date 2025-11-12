import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;



public class Client {
	private int ID;
	private int IP;
	public String pseudo;
	
	private SecretKey cle_aes_prive;
	private byte[] cle_aes_public;
	
	
	
	
	public Client(String pseudo) {
		this.pseudo = pseudo;
	}
	
	/** Ajoute au pseudonyme le nombre en entrée. Permez d'éviter les collisions de pseudo
	 * (cette fonction doit être appelé par le serveur s'il enregistre deux clients aux pseudonymes identiques
	 **/
	public void make_unique_pseudo(int nb){
		this.pseudo = this.pseudo+String.valueOf(nb);
		System.out.println("Votre pseudo a dû être modifié en "+this.pseudo);
	}

	
	
	public static void main(String[] args) {
		String host = PARAMETRE.host;
		int port = PARAMETRE.port + 1;
		SecretKey AES_key;
		Cipher cipher = null;
		try {
			AES_key = echanger_AES();
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, AES_key);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		boolean attendre = true;
		while (attendre) {
			try (Socket socket = new Socket(host, port);
					BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());) {
				attendre = false;
				System.out.println("Connecté au serveur sur " + host + ":" + port);
				System.out.println("Tape un message (ou 'bye' pour quitter) :");

				String message;
				while ((message = console.readLine()) != null) {
					if (message.equalsIgnoreCase("bye")) {
						out.writeInt(0);
						out.flush();
						break;
					}
					try {
						byte[] encrypte = cipher.doFinal(message.getBytes());
						out.writeInt(encrypte.length);
						out.write(encrypte);
						out.flush();
					} catch (IllegalBlockSizeException | BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				System.out.println("Client déconnecté.");
			} catch (IOException e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					e.printStackTrace();
				}

			}
		}
	}

	public static SecretKey echanger_AES()
			throws UnknownHostException, IOException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {

		String host = PARAMETRE.host;
		int port = PARAMETRE.port;
		Socket socket = new Socket(host, port);
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

		// On reçoit la clé publique du serveur
		PublicKey rsaPublique = (PublicKey) in.readObject();

		// On génère sa clé privée
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		SecretKey aesKey = keyGen.generateKey();
		
		
		Cipher cipherRSA = Cipher.getInstance("RSA");
		cipherRSA.init(Cipher.ENCRYPT_MODE, rsaPublique);
		byte[] aesChiffree = cipherRSA.doFinal(aesKey.getEncoded());

		out.writeObject(aesChiffree);
		out.flush();

		System.out.println("Clé AES envoyée au serveur !");
		return aesKey;
	}
	
	
	
	
	// Se connecte au serveur avec le port en paramètre
	public void register() throws UnknownHostException, IOException, NoSuchAlgorithmException, NoSuchPaddingException,
	InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException  {
		assert this.pseudo != null && this.pseudo.length() > 1; // Un pseudo doit etre présent avec de se register
		
		String host = PARAMETRE.host;
		int port = PARAMETRE.port;
		Socket socket = new Socket(host, port);
		
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

		// On récupère la clé du serveur
		PublicKey rsaPublique = (PublicKey) in.readObject();

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		SecretKey aesKey = keyGen.generateKey();

		Cipher cipherRSA = Cipher.getInstance("RSA");
		cipherRSA.init(Cipher.ENCRYPT_MODE, rsaPublique);
		byte[] aesChiffree = cipherRSA.doFinal(aesKey.getEncoded());
		
		out.writeObject(aesChiffree);
		out.flush();

		System.out.println("Clé AES envoyée au serveur ! Vous êtes à présent enregistré");
		
		this.cle_aes_prive = aesKey;
		this.cle_aes_public = aesChiffree;
		
	}

}



