import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class Client {
	private int ID;
	private int IP;
	public String pseudo;

	private PrivateKey cle_prive;
	private PublicKey cle_public;
	private ArrayList<Contact> contacts = new ArrayList<Contact>();
	static final String SEP = PARAMETRE.SEP;

	// COMPOSANTS GRAPHIQUES

	// Fenetre Principale
	private JFrame main_frame;
	private GridBagLayout main_grid;
	private GridBagConstraints c = new GridBagConstraints();
	public static final String BASE_TITLE = "Chaffeur, le super chat sécurisé ! (ne dite jamais quoi)";

	// Menu de gauche : liste des groupes
	private JPanel groupe_list;
	JScrollPane scroll_groupe;
	private ArrayList<JButton> groupe_button_list;
	private JButton add_group_btn;

	// Demandes
	private JButton demandeAlerteButton;

	// Chat actuel
	JTextField chatInput;
	private JPanel chat_panel;
	JLabel historychat;
	JButton send_message;
	JPanel entry_panel;
	JPanel all_for_chat_panel;

	// Historique des messages
	private HashMap<String, ArrayList<JLabel>> historique = new HashMap<String, ArrayList<JLabel>>();
	String actual_chat = "global";
	private JScrollPane scroll_chat;

	public Client(String pseudo)
			throws InvalidKeyException, UnknownHostException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
		this.pseudo = pseudo;

		groupe_button_list = new ArrayList<>();

		// FRAME
		main_frame = new JFrame();
		main_frame.setTitle("Chaffeur, le super chat sécurisé ! (ne dite jamais quoi)");
		main_frame.setSize(800, 600);

		main_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		main_frame.setResizable(true);

		main_grid = new GridBagLayout();
		main_frame.setLayout(main_grid);

		// PANEL DE LISTE
		groupe_list = new JPanel();
		groupe_list.setLayout(new BoxLayout(groupe_list, BoxLayout.Y_AXIS));

		// SCROLL
		scroll_groupe = new JScrollPane(groupe_list);

		// Chat panel
		historique.put("global", new ArrayList<JLabel>());
		act_msg_list().add(new JLabel(
				"Voici le salon global ! Ici vous pouvez discuttez avec tous les nouveaux membres de Chafeur !"));

		chatInput = new JTextField();
		chatInput.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		send_message = new JButton("Envoyer");

		chat_panel = new JPanel();
		chat_panel.setLayout(new BoxLayout(chat_panel, BoxLayout.Y_AXIS));

		scroll_chat = new JScrollPane(chat_panel);

		entry_panel = new JPanel(new BorderLayout());
		entry_panel.add(chatInput, BorderLayout.CENTER);
		entry_panel.add(send_message, BorderLayout.EAST);
		entry_panel.setSize(10, 80);

		chatInput.setMinimumSize(new Dimension(10, 50));
		chatInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		send_message.setMinimumSize(new Dimension(10, 50));
		send_message.setMaximumSize(new Dimension(10, 50));

		all_for_chat_panel = new JPanel(new BorderLayout());

		all_for_chat_panel.add(entry_panel, BorderLayout.SOUTH);
		all_for_chat_panel.add(scroll_chat, BorderLayout.NORTH);

		// CONSTRAINTS
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1.0; // prend toute la
							// hauteur
		main_frame.add(scroll_groupe, c);

		// Colonne droite = 2/3
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 10;
		c.weighty = 1.0;
		main_frame.add(all_for_chat_panel, c);

		// BOUTON
		add_group_btn = new JButton("Nouveau groupe");

		// UPDATE
		update_Group_Scroll_Bar();

		update_actual_chat();

		// AFFICHAGE
		main_frame.setVisible(true);

	}

	private void lancerCreationGroupe(PrintWriter out) {
		String nom_groupe = JOptionPane.showInputDialog(main_frame, "Comment voulez appellez le nouveau groupe ?");
		while (nom_groupe.strip().equals("")) {
			nom_groupe = JOptionPane.showInputDialog(main_frame, "Veuillez écrire quelque chose quand même...");
		}

		// Ouverture de la liste des pseudo avec un truc pour cocher (plus tard)
		String nom_poto = JOptionPane.showInputDialog(main_frame, "Qui ajouter ?");

		ArrayList<String> a = new ArrayList<String>();
		a.add(nom_poto);

		creer_groupe(nom_groupe, a, out);

		update_actual_chat();

		update_Group_Scroll_Bar();

	}

	ArrayList<JLabel> act_msg_list() {
		return historique.get(actual_chat);
	}

	void update_actual_chat() {
		all_for_chat_panel.removeAll();
		chat_panel.removeAll();

		// Ajout dans la liste verticale
		for (JLabel msg : act_msg_list()) {
			chat_panel.add(msg);
		}

		all_for_chat_panel.add(entry_panel, BorderLayout.SOUTH);
		all_for_chat_panel.add(scroll_chat, BorderLayout.NORTH);

		all_for_chat_panel.revalidate();
		all_for_chat_panel.repaint();

	}

	void update_Group_Scroll_Bar() {
		groupe_list.removeAll();
		groupe_button_list.clear();

		// On ajoute le bouton "nouveau groupe"
		groupe_button_list.add(add_group_btn);

		for (Contact groupe_contact : contacts) {
			JButton ngroupe = new JButton(groupe_contact.pseudo);
			groupe_button_list.add(ngroupe);
			ngroupe.addActionListener(e -> {
				actual_chat = ngroupe.getText();
				update_actual_chat();
				update_Group_Scroll_Bar();
			});

			if (ngroupe.getText().equals(actual_chat)) {
				ngroupe.setBackground(Color.BLUE);
				ngroupe.setForeground(Color.white);
			}

		}

		// Ajout dans la liste verticale
		for (JButton btn : groupe_button_list) {
			groupe_list.add(btn);

		}

		groupe_list.revalidate();
		groupe_list.repaint();
	}

	/**
	 * Ajoute au pseudonyme le nombre en entrée. Permez d'éviter les collisions de
	 * pseudo (cette fonction doit être appelé par le serveur s'il enregistre deux
	 * clients aux pseudonymes identiques
	 **/
	public void make_unique_pseudo(int nb) {
		this.pseudo = this.pseudo + String.valueOf(nb);
		updateTitle();
	}

	public static void main(String[] args) throws Exception {

		String host = PARAMETRE.host;
		int port = PARAMETRE.port;
		Client moi = new Client("Gabriel");
		moi.main_frame.setVisible(true);
		try {
			Socket socket = new Socket(host, port);
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			// sur la fermeture de la fenetre ca déconecte
			moi.main_frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					try {
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					moi.main_frame.dispose();
				}
			});

			moi.send_message.addActionListener(e -> {
				try {
					moi.envoie_message("MESSAGE_TO", moi.actual_chat, moi.chatInput.getText(), out);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				moi.update_actual_chat();
				moi.update_Group_Scroll_Bar();
				moi.chatInput.setText("");
			});

			moi.register(out, console, in);

			moi.add_group_btn.addActionListener(e -> {
				moi.lancerCreationGroupe(out);
			});

			// out =
			// BufferedReader in =
			Thread ecoute = new Thread(() -> { // ceci est un thread
				String message;
				try {
					while ((message = in.readLine()) != null) {

						moi.recevoir_message(message, out);

					}
				} catch (SocketException e) {
					System.out.println("je suis plus la");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			});
			ecoute.start();
			/**
			 * Thread clavier = new Thread(() -> {// ceci est un thread mais pas le meme
			 * String message; try { while ((message = console.readLine()) != null) { if
			 * (message.equalsIgnoreCase("bye")) { socket.close();
			 * JOptionPane.showMessageDialog(moi.main_frame, "Au revoir et à bientôt :DD");
			 * } else if (message.toLowerCase().equals("quoi") ||
			 * message.toLowerCase().equals("quoi?") || message.toLowerCase().equals("quoi
			 * ?")) { JOptionPane.showMessageDialog(moi.main_frame, "Feur !");
			 * moi.envoie_message("MESSAGE_TO", "global", "Je suis un gros nullos qui a dit
			 * quoi dites moi tous feur !", out); } else if
			 * (message.contains("creerGroupe")) { String[] tab = message.split("\\|", 3);
			 * moi.creer_groupe(tab[1], new
			 * ArrayList<String>(Arrays.asList(tab[2].split("\\|"))), out); } else if
			 * (message.contains("MP")) { String[] tab = message.split("\\|");
			 * moi.envoie_message("MESSAGE_TO", tab[1], tab[2], out);
			 * 
			 * } else { System.out.print(moi.pseudo + " : "); // au
			 * moi.envoie_message("MESSAGE_TO", "global", message, out); // on met "" car
			 * pour l'instant on // envoit } // groupe global
			 * 
			 * } } catch (InvalidKeyException | NoSuchAlgorithmException |
			 * NoSuchPaddingException | IOException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 * 
			 * });
			 **/
		} catch (ConnectException h) {
			JOptionPane.showMessageDialog(moi.main_frame,
					"Nous sommes navré mais la connexion n'a pas pu être établie. Vérifiez votre IP ainsi que l'état du Réseau");
			moi.main_frame.dispose();
		}
		;
		// clavier.start();

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

	public void updateTitle() {
		String title = Client.BASE_TITLE;
		if (pseudo != "" && pseudo != null) {
			title = title + " | VOTRE PSEUDO : " + (pseudo);
		}
		main_frame.setTitle(title);
	}

	public void register(PrintWriter out, BufferedReader clavier_console, BufferedReader in) throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
		this.cle_public = keyPair.getPublic();
		this.cle_prive = keyPair.getPrivate();
		String pseudo = JOptionPane.showInputDialog(main_frame, "Choisissez votre pseudo !");
		if (pseudo.equals("")) {
			JOptionPane.showMessageDialog(main_frame, "Tu t'appeleras Gabriel alors ahah");
			pseudo = "Gabriel";
		}
		this.envoie_message("CONNECT", "server",
				pseudo + SEP + Base64.getEncoder().encodeToString(this.cle_public.getEncoded()), out);

		recevoir_message(in.readLine(), out);

	}

	public void creer_groupe(String nom, ArrayList<String> liste_ami, PrintWriter out) {
		try {
			String mes_amis = nom;
			for (String ami : liste_ami) {
				mes_amis += SEP + ami;
			}
			this.envoie_message("GROUP1", "server", mes_amis, out);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void envoie_message(String intitule, String destinataire, String message, PrintWriter out)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		System.out.println(intitule + destinataire + message);
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
						act_msg_list().add(new JLabel("Vous : " + message));
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
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			String[] decoupe = message.split(SEP, 4);
			switch (decoupe[0]) {
			case "SET_PSEUDO":
				String[] pseudo_et_cle_et_histo = decoupe[3].split(SEP);
				System.out.println(Arrays.toString(pseudo_et_cle_et_histo));
				this.pseudo = pseudo_et_cle_et_histo[0];
				SecretKey clef = funcs.RSA_DECRYPT(pseudo_et_cle_et_histo[1], this.cle_prive);
				this.contacts.add(new Contact("global", clef));
				JOptionPane.showMessageDialog(main_frame, "Votre pseudo a été définit comme étant : " + this.pseudo);
				cipher.init(Cipher.DECRYPT_MODE, clef);
				for (int i = 2; i < pseudo_et_cle_et_histo.length; i++) {
					String[] tab = pseudo_et_cle_et_histo[i].split(":", 2);
					this.historique.get("global").add(new JLabel(tab[0] + ": "
							+ new String(cipher.doFinal(Base64.getDecoder().decode(tab[1])), StandardCharsets.UTF_8)));
				}
				update_Group_Scroll_Bar();
				updateTitle();
				update_actual_chat();
				break;
			case "MESSAGE_FROM":

				for (Contact mon_ami : contacts) {
					if (mon_ami.pseudo.equals(decoupe[2])) {
						// Decrypte le message et l'affiche
						cipher.init(Cipher.DECRYPT_MODE, mon_ami.la_clef);
						byte[] valeur = Base64.getDecoder().decode(decoupe[3]);
						String msg_final = ("> " + decoupe[1] + ": "
								+ new String(cipher.doFinal(valeur), StandardCharsets.UTF_8));
						historique.get(decoupe[2]).add(new JLabel(msg_final));

						if (decoupe[2].equals(actual_chat)) {
							update_actual_chat();
						}

						cond = false;
						break;

					}
				}
				if (cond) {
					// céer le contact
					System.out.println("ne doit jamais arriver");
				}
				break;
			case "GROUP2":
				String[] nom_et_membre_ban = decoupe[3].split(SEP, 2);
				JOptionPane.showMessageDialog(main_frame, "Votre nom de groupe est :" + nom_et_membre_ban[0]);
				if (nom_et_membre_ban[1] != null) {
					System.out.println("Les membres suivant n'existe pas : " + nom_et_membre_ban[1].replace(SEP, " "));
				}
				break;

			case "GROUP3":

				// création du groupe
				String[] nom_et_cle = decoupe[3].split(SEP);
				// System.out.println("new serv"+ nom_et_cle[0]);
				this.contacts.add(new Contact(nom_et_cle[0], funcs.RSA_DECRYPT(nom_et_cle[1], this.cle_prive)));
				this.historique.put(nom_et_cle[0], new ArrayList<JLabel>());

				// System.out.println("Vous etes dans le groupe : " + nom_et_cle[0]);
				update_Group_Scroll_Bar();

				break;
			default:
				System.out.println(message);
				break;

			}

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |

				BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
