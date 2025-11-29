import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Client {
	private int ID;
	private int IP;
	public String pseudo;

	private PrivateKey cle_prive;
	private PublicKey cle_public;
	private ArrayList<Contact> contacts = new ArrayList<Contact>();
	static final String SEP = PARAMETRE.SEP;

	public Client(String pseudo)
			throws InvalidKeyException, UnknownHostException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
		this.pseudo = pseudo;

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

	public static void main(String[] args) throws Exception {
		String host = PARAMETRE.host;
		int port = PARAMETRE.port;
		Client moi = new Client("Gabriel");
		Socket socket = new Socket(host, port);
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		moi.register(out, console, in);

		// out =
		// BufferedReader in =
		Thread ecoute = new Thread(() -> { // ceci est un thread
			String message;
			try {
				while ((message = in.readLine()) != null) {

					moi.recevoir_message(message, out);

				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
		ecoute.start();
		Thread clavier = new Thread(() -> {// ceci est un thread mais pas le meme
			String message;
			try {
				while ((message = console.readLine()) != null) {
					if (message.equalsIgnoreCase("bye")) {
						socket.close();
						System.out.println("Au revoir et à bientôt :DD");
					} else if (message.toLowerCase().equals("quoi") || message.toLowerCase().equals("quoi?")
							|| message.toLowerCase().equals("quoi ?")) {
						System.out.println("Feur !");
						moi.envoie_message("MESSAGE_TO", "global",
								"Je suis un gros nullos qui a dit quoi dites moi tous feur !", out);
					} else {
						System.out.print(moi.pseudo + " : "); // au
						moi.envoie_message("MESSAGE_TO", "global", message, out); // on met "" car pour l'instant on
																					// envoit
					}
					// groupe global

				}
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
		clavier.start();

	}

	/**
	 * public static SecretKey echanger_AES() throws UnknownHostException,
	 * IOException, NoSuchAlgorithmException, NoSuchPaddingException,
	 * InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
	 * ClassNotFoundException {
	 * 
	 * String host = PARAMETRE.host; int port = PARAMETRE.port; Socket socket = new
	 * Socket(host, port); ObjectOutputStream out = new
	 * ObjectOutputStream(socket.getOutputStream()); ObjectInputStream in = new
	 * ObjectInputStream(socket.getInputStream());
	 * 
	 * // On reçoit la clé publique du serveur PublicKey rsaPublique = (PublicKey)
	 * in.readObject();
	 * 
	 * // On génère sa clé privée KeyGenerator keyGen =
	 * KeyGenerator.getInstance("AES"); keyGen.init(128); SecretKey aesKey =
	 * keyGen.generateKey();
	 * 
	 * Cipher cipherRSA = Cipher.getInstance("RSA");
	 * cipherRSA.init(Cipher.ENCRYPT_MODE, rsaPublique); byte[] aesChiffree =
	 * cipherRSA.doFinal(aesKey.getEncoded());
	 * 
	 * out.writeObject(aesChiffree); out.flush();
	 * 
	 * System.out.println("Clé AES envoyée au serveur !"); return aesKey; }
	 **/

	// Se connecte au serveur avec le port en paramètre
	public void register(PrintWriter out, BufferedReader clavier_console, BufferedReader in) throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
		this.cle_public = keyPair.getPublic();
		this.cle_prive = keyPair.getPrivate();
		System.out.println("Choisissez votre peusdo : ");
		String pseudo = clavier_console.readLine();
		if (pseudo.equals("")) {
			System.out.println("Tu t'appeleras Gabriel alors ahah");
			pseudo = "Gabriel";
		}
		this.envoie_message("CONNECT", "server",
				pseudo + SEP + Base64.getEncoder().encodeToString(this.cle_public.getEncoded()), out);

		recevoir_message(in.readLine(), out);
		System.out.println("Bienvenu dans le chafeur:");

	}

	public void creer_groupe(String nom, ArrayList<String> liste_ami, PrintWriter out) {
		try {
			String mes_amis = "";
			for (String ami : liste_ami) {
				mes_amis += ami + SEP;
			}
			this.envoie_message("groupe_etape_1", nom, mes_amis, out);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void envoie_message(String intitule, String destinataire, String message, PrintWriter out)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		if (destinataire.equals("server")) {
			out.println(intitule + SEP + this.pseudo + SEP + "server" + SEP + message);
		} else {
			for (Contact mon_ami : contacts) {// recherche de mon ami
				if (mon_ami.pseudo.equals(destinataire)) {// si j'ai un ami
					try {
						Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						cipher.init(Cipher.ENCRYPT_MODE, mon_ami.la_clef);
						byte[] encrypte = cipher.doFinal(message.getBytes());
						if (intitule.equals(null)) {
							intitule = "MESSAGE_TO";
						}
						out.println(intitule + SEP + this.pseudo + SEP + destinataire + SEP
								+ Base64.getEncoder().encodeToString(encrypte));
					} catch (IllegalBlockSizeException | BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

		}
	}

	public void recevoir_message(String message, PrintWriter out) throws Exception {
		try {
			boolean cond = true;
			String[] decoupe = message.split(SEP, 4);
			switch (decoupe[0]) {
			case "SET_PSEUDO":
				String[] pseudo_et_cle = decoupe[3].split(SEP);
				this.pseudo = pseudo_et_cle[0];
				this.contacts.add(new Contact("global", funcs.RSA_DECRYPT(pseudo_et_cle[1], this.cle_prive)));
				System.out.println("Votre pseudo a été définit comme étant : " + this.pseudo);
				break;
			case "MESSAGE_FROM":

				for (Contact mon_ami : contacts) {
					if (mon_ami.pseudo.equals(decoupe[2])) {
						// Decrypte le message et l'affiche
						Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						cipher.init(Cipher.DECRYPT_MODE, mon_ami.la_clef);
						byte[] valeur = Base64.getDecoder().decode(decoupe[3]);
						System.out.println(mon_ami.pseudo + " " + decoupe[1] + ": "
								+ new String(cipher.doFinal(valeur), StandardCharsets.UTF_8));
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
				for (String clef_public : decoupe[2].split(SEP)) {
					cipher.init(Cipher.ENCRYPT_MODE,
							new SecretKeySpec(clef_public.getBytes(StandardCharsets.UTF_8), "RSA"));
					liste_clefs += cipher.doFinal(aesKey.getEncoded()) + SEP;

				}
				this.envoie_message("groupe_etape_3", decoupe[1], liste_clefs, out);

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

}
