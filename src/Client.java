import java.io.BufferedReader;
import java.io.DataOutputStream;
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
<<<<<<< Updated upstream
=======
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
>>>>>>> Stashed changes

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
<<<<<<< Updated upstream
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
=======
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
>>>>>>> Stashed changes

public class Client {
	private int ID;
	private int IP;
	public String pseudo;

	private PrivateKey cle_prive;
	private PublicKey cle_public;
	private ArrayList<Contact> contacts = new ArrayList<Contact>();

	public Client(String pseudo)
			throws InvalidKeyException, UnknownHostException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
		this.pseudo = pseudo;
<<<<<<< Updated upstream
		register();
=======

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
		// ajoute ici ton historychat, chatInput, send_message, etc.
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
	public Contact get_group(String group_name) {
		for (Contact c : contacts) {
			if (c.pseudo.equals(group_name)) return c;
		}
		return new Contact("", null);
	}
	
	public String[] get_other_users(String group_name) {
		
		
		for (Contact c : contacts) {
			if (c.pseudo.equals(group_name)) {
				ArrayList<String> list_others = new ArrayList<String>();
				for (String user : c.users) {
					if (!user.equals(this.pseudo)) list_others.add(user);
				}
				String[] res = new String[list_others.size()];
				for (int i = 0; i<list_others.size(); i++) {
					res[i] = list_others.get(i);
				}
				return res;
				
				
			}
		}
		return new String[0];
		
	}

	private void lancerCreationGroupe(PrintWriter out, String[] people) {
		String nom_groupe = JOptionPane.showInputDialog(main_frame, "Comment voulez appellez le nouveau groupe ?");
		while (nom_groupe.strip().equals("")) {
			nom_groupe = JOptionPane.showInputDialog(main_frame, "Veuillez écrire quelque chose quand même...");
		}
		
		final String nom_groupe_def = nom_groupe;

		// Objectif : changer cet fenetre de dialogue en une liste de tous les pseudos
		// Ouverture de la liste des pseudo avec un truc pour cocher (plus tard)
		// String nom_poto = JOptionPane.showInputDialog(main_frame, "Qui ajouter ?");
		
		JDialog dialog = new JDialog(main_frame, "Qui ajouter au groupe ?", true);
        dialog.setLayout(new BorderLayout());
        JPanel panelListe = new JPanel();
        panelListe.setLayout(new BoxLayout(panelListe, BoxLayout.Y_AXIS));

        List<JCheckBox> checkboxes = new ArrayList<>();
        for (String s : people) {
            JCheckBox cb = new JCheckBox(s);
            checkboxes.add(cb);
            panelListe.add(cb);
        }

        JButton valider = new JButton("Valider");
        valider.addActionListener(e -> {
            ArrayList<String> selections = new ArrayList<String>();
            for (JCheckBox cb : checkboxes) {
                if (cb.isSelected()) {
                    selections.add(cb.getText());
                }
            }
            creer_groupe(nom_groupe_def, selections, out);
            dialog.dispose();
        });

        dialog.add(new JScrollPane(panelListe), BorderLayout.CENTER);
        dialog.add(valider, BorderLayout.SOUTH);

        dialog.setSize(300, 300);
        dialog.setLocationRelativeTo(main_frame);
        dialog.setVisible(true);

		

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
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
		Cipher cipherRSA = Cipher.getInstance("RSA");
		cipherRSA.init(Cipher.ENCRYPT_MODE, rsaPublique);
		byte[] aesChiffree = cipherRSA.doFinal(aesKey.getEncoded());
=======
		moi.add_group_btn.addActionListener(e -> {
			moi.lancerCreationGroupe(out, moi.get_other_users("global"));
		});
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
				byte[] valeur = decoupe[2].getBytes(StandardCharsets.UTF_8);
				Cipher cipher1 = Cipher.getInstance("RSA");
				cipher1.init(Cipher.DECRYPT_MODE, this.cle_prive);
				Contact nouvel_ami = new Contact(decoupe[1], new SecretKeySpec(cipher1.doFinal(valeur), "AES"));
				contacts.add(nouvel_ami);
=======
				String[] nom_et_cle_et_membres = decoupe[3].split(SEP);
				// System.out.println("new serv"+ nom_et_cle[0]);
				Contact nouveau_contact = new Contact(nom_et_cle_et_membres[0], funcs.RSA_DECRYPT(nom_et_cle_et_membres[1], this.cle_prive));
				
				this.historique.put(nom_et_cle_et_membres[0], new ArrayList<JLabel>());
				
				String[] membres = nom_et_cle_et_membres[2].split("\\|");
				for (String membre : membres) {
					nouveau_contact.users.add(membre);
				}
				
				this.contacts.add(nouveau_contact);
				

				// System.out.println("Vous etes dans le groupe : " + nom_et_cle[0]);
				update_Group_Scroll_Bar();

				break;
			case "UPDATE_GROUP":
				
				String groupe = decoupe[1];
				String[] membres1 = decoupe[2].split("\\|");
				System.out.println("J'update le groupe "+groupe);
				Contact groupe_trouve;
				if ((groupe_trouve=get_group(groupe)) != null) {
					groupe_trouve.users = new ArrayList<String>(Arrays.asList(membres1));
				}
				
			default:
				System.out.println(message);
>>>>>>> Stashed changes
				break;
			

			}

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
