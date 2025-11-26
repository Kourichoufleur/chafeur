import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Client implements Runnable {
	public volatile String pseudo;

	private volatile PrivateKey cle_prive;
	private volatile PublicKey cle_public;
	private volatile ArrayList<Contact> contacts = new ArrayList<Contact>();

	public Client(String pseudo)
			throws InvalidKeyException, UnknownHostException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
		this.pseudo = pseudo;
		register();
	}

	/**
	 * Ajoute au pseudonyme le nombre en entrée. Permez d'éviter les collisions de
	 * pseudo (cette fonction doit être appelé par le serveur s'il enregistre deux
	 * clients aux pseudonymes identiques
	 **/
	public void make_unique_pseudo(int nb) {
		this.pseudo = this.pseudo + String.valueOf(nb);
		System.out.println("Votre pseudo a dû être modifié en " + this.pseudo);
	}

	private static String demander_pseudo() {
		return "Dieu tout puissant venerer par tous";
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
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();

		this.cle_public = keyPair.getPublic();
		this.cle_prive = keyPair.getPrivate();

		this.envoie_message("CONNECT", "server",
				this.pseudo + "|" + new String(this.cle_public.getEncoded(), StandardCharsets.UTF_8));
	}

	public void creer_groupe(String nom, ArrayList<String> liste_ami) {
		try {
			String mes_amis = "";
			for (String ami : liste_ami) {
				mes_amis += ami + "|";
			}
			this.envoie_message("groupe_etape_1", nom, mes_amis);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void envoie_message(String intitule, String pseudo, String message)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (pseudo.equals("server")) {
			String host = PARAMETRE.host;
			int port = PARAMETRE.port;
			try (Socket socket = new Socket(host, port);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);) {

				System.out.println("Connecté au serveur sur " + host + ":" + port);
				out.println(intitule + "|server|" + message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
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
							if (intitule.equals(null)) {
								intitule = "MESSAGE";
							}
							out.println(intitule + "|" + mon_ami.pseudo + "|"
									+ new String(encrypte, StandardCharsets.UTF_8));
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

	}

	public void recevoir_message(String message) {
		try {
			boolean cond = true;
			String[] decoupe = message.split("|", 3);
			switch (decoupe[0]) {
			case "message":
				for (Contact mon_ami : contacts) {
					if (mon_ami.pseudo.equals(decoupe[1])) {
						// Decrypte le message et l'affiche
						Cipher cipher = Cipher.getInstance("AES");
						cipher.init(Cipher.DECRYPT_MODE, mon_ami.la_clef);
						byte[] valeur = decoupe[2].getBytes(StandardCharsets.UTF_8);
						System.out.println(mon_ami.pseudo + ":\n" + cipher.doFinal(valeur));
						cond = false;
						break;

					}
				}
				if (cond) {
					// céer le contact
					System.out.println("ne doit jamais arriver");
				}
				break;
			case "groupe_etape_2":
				// genere la clef aes l'encrypte avec chaque clef publique et envoie un message
				// avec groupe_etape_3 le nom du groupe et les clefs aes séparé par des pipes
				KeyGenerator keyGen = KeyGenerator.getInstance("AES");
				keyGen.init(128);
				SecretKey aesKey = keyGen.generateKey();
				contacts.add(new Contact(decoupe[1], aesKey));
				String liste_clefs = "";
				Cipher cipher = Cipher.getInstance("RSA");
				for (String clef_public : decoupe[2].split("|")) {
					cipher.init(Cipher.ENCRYPT_MODE,
							new SecretKeySpec(clef_public.getBytes(StandardCharsets.UTF_8), "RSA"));
					liste_clefs += cipher.doFinal(aesKey.getEncoded()) + "|";

				}
				this.envoie_message("groupe_etape_3", decoupe[1], liste_clefs);

			case "groupe_etape_final":
				// création du groupe
				byte[] valeur = decoupe[2].getBytes(StandardCharsets.UTF_8);
				Cipher cipher1 = Cipher.getInstance("RSA");
				cipher1.init(Cipher.DECRYPT_MODE, this.cle_prive);
				Contact nouvel_ami = new Contact(decoupe[1], new SecretKeySpec(cipher1.doFinal(valeur), "AES"));
				contacts.add(nouvel_ami);
				break;

			}

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		String host = PARAMETRE.host;
		int port = PARAMETRE.port;
		// tentative de conexion au serveur
		try (Socket socket = new Socket(host, port);
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
			String message;
			while ((message = in.readLine()) != null) {
				this.recevoir_message(message);

			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args)
			throws InvalidKeyException, UnknownHostException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
		Client moi = new Client(Client.demander_pseudo());
		Thread moi_thread = new Thread(moi);
		moi_thread.start();

		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		String message;
		String a_qui = "Louis";
		try {
			while ((message = console.readLine()) != null) {
				if (message.equalsIgnoreCase("bye")) {
				}
				try {
					moi.envoie_message("MESSAGE_TO", a_qui, message);
				} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
