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
import java.util.ArrayList;

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
	public Client(String pseudo) {
		this.pseudo = pseudo;
	}

	 **/
	public void make_unique_pseudo(int nb) {
		this.pseudo = this.pseudo + String.valueOf(nb);
		System.out.println("Votre pseudo a dû être modifié en " + this.pseudo);
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
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
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

	}

	public void envoie_message(String message, String pseudo)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		for (Contact mon_ami : contacts) {// recherche de mon ami
			if (mon_ami.pseudo.equals(pseudo)) {// si j'ai un ami
				String host = PARAMETRE.host;
				int port = PARAMETRE.port;
				// tentative de conexion au serveur
				try (Socket socket = new Socket(host, port);
						PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {

					System.out.println("Connecté au serveur sur " + host + ":" + port);
					System.out.println("Tape un message (ou 'bye' pour quitter) :");

					if (message.equalsIgnoreCase("bye")) {
						out.println("bye||");
						int[] tab = { 0 };
						int bidule = tab[1];// euh non mais tkt
					}
					try {
						Cipher cipher = Cipher.getInstance("AES");
						cipher.init(Cipher.ENCRYPT_MODE, mon_ami.la_clef);
						byte[] encrypte = cipher.doFinal(message.getBytes());
						out.println(
								"message" + "|" + mon_ami.pseudo + "|" + new String(encrypte, StandardCharsets.UTF_8));
					} catch (IllegalBlockSizeException | BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} catch (IOException e) {
					e.printStackTrace();
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Je suis parti");
				}

			}
		}

	}

	public void recevoir_message(String message) {
		try {
			boolean cond = true;
			String[] decoupe = message.split("|", 3);
			if (decoupe[1] == "server") {
				// gerer les message server
			} else {
				switch (decoupe[0]) {
				case "message":
					for (Contact mon_ami : contacts) {
						if (mon_ami.pseudo.equals(decoupe[1])) {
							Cipher cipher = Cipher.getInstance("AES");
							cipher.init(Cipher.DECRYPT_MODE, mon_ami.la_clef);
							byte[] valeur = decoupe[2].getBytes(StandardCharsets.UTF_8);
							System.out.println(cipher.doFinal(valeur));
							cond = false;
							break;

						}
					}
					if (cond) {
						// créer le contact

					}
					break;
				case "demande":
					byte[] valeur = decoupe[2].getBytes(StandardCharsets.UTF_8);
					Cipher cipher = Cipher.getInstance("RSA");
					cipher.init(Cipher.DECRYPT_MODE, this.cle_prive);
					Contact nouvel_ami = new Contact(decoupe[1], new SecretKeySpec(cipher.doFinal(valeur), "AES"));
					contacts.add(nouvel_ami);
					break;

				}
			}

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
