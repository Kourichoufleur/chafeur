import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**La classe Client est la classe que le client doit lancer. Elle gère l'interface graphique de l'application, et permet de se connecter au serveur dont l'IP est définit dans PARAMETRE
 **/
public class Client {

	public String pseudo;

	// Attributs de cryptage
	private PrivateKey cle_prive;
	private PublicKey cle_public;
	static final String SEP = PARAMETRE.SEP; // Caractère UNICODE séparant chaque morceau du message (catégorie, destinataire, etc...)
	
	// Les contacts servent à stocker tous les groupes : leurs clés, leur membres, et leur nom
	private ArrayList<Contact> contacts = new ArrayList<Contact>();
	

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

	// Chat actuel
	JTextField chatInput;
	private JPanel chat_panel;
	JLabel historychat;
	JButton send_message;
	JPanel group_info_panel;
	JButton info_membre_group;
	JButton quitter_groupe;
	JButton ajouter_membre;
	JPanel entry_panel;
	JPanel all_for_chat_panel;

	// Historique des messages
	private HashMap<String, ArrayList<JLabel>> historique = new HashMap<String, ArrayList<JLabel>>();
	String actual_chat = "global";
	private JScrollPane scroll_chat;
	private JPanel leave_and_info_panel;
	private JButton quitter_app_btn;
	
	// Petit message piégé si l'utilisateur finit "quoi"
	private final static String PRANK_MSG =  "Coucou répondez moi tous à ce message par 'feur', j'adore ça !!!!";

	
	/**La classe Client est la classe que le client doit lancer. Le constructeur lance l'interface graphique mais ne se connecte pas automatiquement au serveur
	 **/
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
				"Voici le groupe global ! Ici vous pouvez discuttez avec tous les nouveaux membres de Chafeur !"));

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
		
		
		
		
		// Group info
		group_info_panel = new JPanel(new BorderLayout());
		quitter_groupe = new JButton("Quitter");
		ajouter_membre = new JButton("Ajouter");
		info_membre_group = new JButton("Membres");
		
		leave_and_info_panel = new JPanel(new BorderLayout());
		quitter_app_btn = new JButton("Fermer");
		quitter_app_btn.setBackground(Color.RED);
		quitter_app_btn.setForeground(Color.WHITE);
		
		
		group_info_panel.add(info_membre_group, BorderLayout.WEST);
		group_info_panel.add(ajouter_membre, BorderLayout.CENTER);
		group_info_panel.add(quitter_groupe, BorderLayout.EAST);
		
		leave_and_info_panel.add(group_info_panel, BorderLayout.CENTER);
		leave_and_info_panel.add(quitter_app_btn, BorderLayout.EAST);
		
		
		chatInput.setMinimumSize(new Dimension(10, 50));
		chatInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
		send_message.setMinimumSize(new Dimension(10, 50));
		send_message.setMaximumSize(new Dimension(10, 50));

		all_for_chat_panel = new JPanel(new BorderLayout());

		all_for_chat_panel.add(entry_panel, BorderLayout.SOUTH);
		all_for_chat_panel.add(scroll_chat, BorderLayout.NORTH);

		// c.___ pour definir les paramètre d'ajout dans une GridBagConstraints (une grille plus personnalisable que le simple GridLayout)
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1.0; 
		main_frame.add(scroll_groupe, c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 10;
		c.weighty = 1.0;
		main_frame.add(all_for_chat_panel, c);

		add_group_btn = new JButton("Nouveau groupe");

		// UPDATE
		switch_to_groupe("global");

		// AFFICHAGE
		main_frame.setVisible(true);

	}
	
	/**
	 * Renvoit le groupe (Contact) lié au contact par son nom passé en paramètre. Renvoit un contact vide si le groupe n'existe pas !!!
	 */
	public Contact get_group(String group_name) {

		for (Contact c : contacts) {
			if (c.pseudo.equals(group_name)) return c;
		}
		return new Contact("", null);
	}
	
	/**
	 * Permet d'obtenir un tableau de String contenant le nom de tout les membres d'un groupe (dont le nom est passé en paramètre) excepté vous même
	 **/
	public String[] get_other_users(String group_name) {

		for (Contact c : contacts) {
			if (c.pseudo.equals(group_name)) {
				ArrayList<String> list_others = new ArrayList<String>();
				for (String user : c.users) {
					if (!user.equals(this.pseudo)) list_others.add(user); // j'ajoute tout utilsateur dont le pseudo est différent de moi
				}
				
				// Je le convertit en tableau
				String[] res = new String[list_others.size()];
				for (int i = 0; i<list_others.size(); i++) {
					res[i] = list_others.get(i);
				}
				return res;
				
				
			}
		}
		return new String[0];
		
	}
	
	
	/**
	 * Ouvre une fenêtre demandant d'entrer un prénom puis une fenêtre de selection multiple contenant les membres présent dans people. Lance la création d'une groupe
	 * contenant les membres choisis
	 **/
	private void lancerCreationGroupe(PrintWriter out, String[] people) {
		/**
		 * Ouvre une fenêtre demandant d'entrer un prénom puis une fenêtre de selection multiple contenant les membres présent dans people. Lance la création d'une groupe
		 * contenant les membres choisis
		 **/
		String nom_groupe = JOptionPane.showInputDialog(main_frame, "Comment voulez appellez le nouveau groupe ?");
		while (nom_groupe.strip().equals("")) {
			nom_groupe = JOptionPane.showInputDialog(main_frame, "Veuillez écrire quelque chose quand même...");
		}
		
		final String nom_groupe_def = nom_groupe;


		JDialog dialog = new JDialog(main_frame, "Qui ajouter au groupe ?", true);
        dialog.setLayout(new BorderLayout());
        JPanel panelListe = new JPanel();
        panelListe.setLayout(new BoxLayout(panelListe, BoxLayout.Y_AXIS));
        
        // Pour chaque personne dans people, j'ajoute une checkbox dans la liste de checkboxes et je la place dans le panelListe
        List<JCheckBox> checkboxes = new ArrayList<>();
        for (String s : people) {
            JCheckBox cb = new JCheckBox(s);
            checkboxes.add(cb);
            panelListe.add(cb);
        }

        JButton valider = new JButton("Valider");
        
        valider.addActionListener(e -> {
            ArrayList<String> selections = new ArrayList<String>();
            
            // J'analyse chaque checkbox pour permettre d'avoir  la liste des personne selectionnées
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
	
	/**
	 * Renvoit la liste de messages affiché actuellement (les messages sont stockés sous forme de JLabel
	 */
	ArrayList<JLabel> act_msg_list() {
		return historique.get(actual_chat);
	}
	
	
	/**
	 * Met à jour le chat en fonction du chat actuel. A appeler à chaque fois qu'un message est reçu ou que vous changez de groupe actuel
	 */
	void update_actual_chat() {
		all_for_chat_panel.removeAll();
		chat_panel.removeAll();

		// Ajout dans la liste verticale
		for (JLabel msg : act_msg_list()) {
			chat_panel.add(msg);
		}
		
		
		
		all_for_chat_panel.add(leave_and_info_panel, BorderLayout.NORTH);
		all_for_chat_panel.add(entry_panel, BorderLayout.SOUTH);
		all_for_chat_panel.add(scroll_chat, BorderLayout.CENTER);

		all_for_chat_panel.revalidate();
		all_for_chat_panel.repaint();
	}
	
	
	/**
	 * Lance une fenêtre affichant tous les membres actuel du groupe indiqué en entrée
	 */
	void afficher_membre(String groupe) {
		Contact trouve = get_group(groupe);
		if (trouve != null) {
			JDialog dialog = new JDialog(main_frame, "Voici la liste des membres du groupe "+groupe, true);
			String[] people = get_other_users(groupe); // Ceci me renvoit tous les membres du meme groupe sauf moi meme (que je traite à part)
			
	        dialog.setLayout(new BorderLayout());
	        JPanel panelListe = new JPanel();
	        panelListe.setLayout(new BoxLayout(panelListe, BoxLayout.Y_AXIS));
	        
	        panelListe.add(new JLabel(this.pseudo + " (Vous)")); // Je me gère en cas à part
	        for (String s : people) {
	            JLabel cb = new JLabel("> "+s); // Je rajoute 1 label par ligne
	            panelListe.add(cb);
	        }

	        JButton sortir = new JButton("OK");
	        sortir.addActionListener(e -> {
	            dialog.dispose();
	        });

	        dialog.add(new JScrollPane(panelListe), BorderLayout.CENTER);
	        dialog.add(sortir, BorderLayout.SOUTH);

	        dialog.setSize(350, 250);
	        dialog.setLocationRelativeTo(main_frame);
	        dialog.setVisible(true);
		}
	}
	
	/**
	 * Envoie un message de déconnexion, et se déconnecte de la Socket. Le serveur prendra le relai pour afficher aux autres clients que ce client s'est déconnecté  
	 * @param out Le PrintWriter où sera écrit le message de déconnection. Doit être relié à un ClientRegistration de la machine de l'hôte pour que les autres utilisateurs puissent bien voir ce Client se déconnecter
	 */
	void deconnecter(PrintWriter out, Socket host) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		
		String message = JOptionPane.showInputDialog(main_frame, "Vous partez déjà ?! Un dernier mot ? (entrez 'annuler' pour ne pas quitter)");
		
		if (!message.toLowerCase().equals("annuler")) {
			envoie_message("DISCONNECT","server",message,out);
			try  {
			host.close();
			}
			finally {} ;
			System.exit(0);
		}
		
		
	}
	
	/**
	 * Fais quitter le groupe dont le nom est passé en paramètre. Cela ne vous fais pas quitter de l'application, seulement du groupe
	 * @param out Le PrintWriter où sera écrit la commande de retrait de groupe. Doit être relié à un ClientRegistration de la machine de l'hôte pour que les autres utilisateurs puissent bien voir ce Client se déconnecter

	 */
	void partir_groupe(String groupe, PrintWriter out) {
		if (groupe.equals("global")) {
			JOptionPane.showMessageDialog(main_frame, "Vous ne pouvez pas quitter le groupe publique principal !");
		}
		else {
		JDialog dialog = new JDialog(main_frame, "Êtes vous sûr de vouloir partir du groupe "+groupe+"? Vous leur manquerez !", true);
		dialog.setLayout(new BorderLayout());
        JPanel panelListe = new JPanel();
        String[] people = get_other_users(groupe); // je récupère la liste des membres pour générer une petite fenetre avec chaque membre qui le supplit de rester
        panelListe.setLayout(new BoxLayout(panelListe, BoxLayout.Y_AXIS));
        // La liste des différents messages possibles
        String[] message_de_tristesse = {" pleurera votre départ", " vous supplie de rester", " implore votre clémence", " veut vous garder à ses côté", " écrira un article sur votre départ", " menace de quitter si vous quitter aussi", " en parlera à son psy", " présent le pire en votre absence", " a besoin de votre réconfort", " est prêt à partir à votre place"};
        
        int i = 0;
        for (String s : people) {
            JLabel cb = new JLabel("> "+s+message_de_tristesse[(i++)%message_de_tristesse.length]); // je créer un label par ligne (= par personne)
            panelListe.add(cb);
        }

        JButton sortir = new JButton("Oui !!");
        sortir.addActionListener(e -> {
            dialog.dispose();
            out.println("LEAVE_GROUP"+ SEP + groupe + SEP + SEP); // On envoie au serveur une requete lui disant de quitter le groupe 'groupe'
            try {historique.remove(groupe);}
            finally {};
            
            try {contacts.remove(get_group(groupe));}
            finally {};
            
            switch_to_groupe("global"); // On retourne au groupe par défaut (le groupe global/publique)
    		
        });
        
        JButton rester = new JButton("Non");
        rester.addActionListener(e -> {
            dialog.dispose();
            // ouf !
        });
        
        JPanel lesdeuxoptions = new JPanel(new BorderLayout());
        lesdeuxoptions.add(sortir, BorderLayout.WEST);
        lesdeuxoptions.add(rester, BorderLayout.EAST);

        dialog.add(new JScrollPane(panelListe), BorderLayout.CENTER);
        dialog.add(lesdeuxoptions, BorderLayout.SOUTH);

        dialog.setSize(550, 250);
        dialog.setLocationRelativeTo(main_frame);
        dialog.setVisible(true);
		}
	}
	
	/**
	 * Lancer une fenêtre permettant de sélectionner des membres non présent dans le groupe passé en paramètre, puis d'ajouter les membres selectionnés au groupe
	 * @param out Le PrintWriter où sera écrit la commande d'ajout de membre. Doit être relié à un ClientRegistration de la machine de l'hôte pour que les autres utilisateurs puissent bien voir ce Client se déconnecter
	 */
	void ajouter_membre(String groupe, PrintWriter out) {
		
		Contact contact = get_group(groupe);
		if (contact != null) {
		
		JDialog dialog = new JDialog(main_frame, "Qui ajouter au groupe ?", true);
        dialog.setLayout(new BorderLayout());
        JPanel panelListe = new JPanel();
        panelListe.setLayout(new BoxLayout(panelListe, BoxLayout.Y_AXIS));

        List<JCheckBox> checkboxes = new ArrayList<>(); // Je génère une liste de checkbox. une pour chaque utilisateur pas déjà dans le groupe
        for (String s : get_other_users("global")) {
        	if (!contact.users.contains(s)) {
        		JCheckBox cb = new JCheckBox(s);
                checkboxes.add(cb);
                panelListe.add(cb);
        	}
        }

        JButton valider = new JButton("Valider");
        valider.addActionListener(e -> {
            String selections = "";
            boolean found=false;
            for (JCheckBox cb : checkboxes) { // Je récupère la selection
                if (cb.isSelected()) {
                    selections += (cb.getText())+"|"; // Je la convertit en string de type membre1|membre2|...|membre_n
                    found=true; // ca me dit  : ok on rajoute bien des gens (au moins 1)
                }
            }
            if (found) {
            	selections = selections.substring(0, selections.length()-1); // J'enlève le dernier | car il est en trop (pour le format que l'on a créé)
            	out.println("ADD_TO_GROUP"+SEP+groupe+SEP+SEP+selections); // J'envoie au serveur (à ma ClientRegistration lié) la demande d'ajout de membres
            }
            
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
	}
	
	/**
	 * Modifie le groupe actuellement selectionné par le client vers le groupe selectionné en paramètre
	 */
	void switch_to_groupe(String groupe) {
		actual_chat = groupe;
		
		// Je retire la possibilité de quitter ou ajouter des membres si on se déplace vers le salon Global (car ça n'a pas de logique)
		quitter_groupe.setEnabled(!(groupe.equals("global"))); 
		ajouter_membre.setEnabled(!(groupe.equals("global")));
		
        update_Group_Scroll_Bar();
        update_actual_chat();
	}
	
	
	
	
	/**
	 * Met à jour l'interface du menu des groupes. Affiche le bleu le bouton du groupe actuel.
	 */
	void update_Group_Scroll_Bar() {
		groupe_list.removeAll();
		groupe_button_list.clear();

		// J'ajoute le bouton "nouveau groupe"
		groupe_button_list.add(add_group_btn);

		for (Contact groupe_contact : contacts) {
			JButton ngroupe = new JButton(groupe_contact.pseudo);
			groupe_button_list.add(ngroupe);
			ngroupe.addActionListener(e -> {
				switch_to_groupe(ngroupe.getText()); // J'applique à chaque bouton la fonction qui switch le groupe actuel vers le groupe indiqué sur le bouton
			});
			
			// Je trouve le bouton représentant le chat actuel pour le mettre en bleu
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
	 * Lance une fenêtre client, tente de se connecter et modifie les boutons pour qu'ils puissent communiquer avec le serveur en cliquant dessus
	 */
	public static void main(String[] args) throws Exception {

		String host = PARAMETRE.host;
		int port = PARAMETRE.port;
		Client moi = new Client("Gabriel"); // Gabriel est un place holder, c'est remplacé après (nous aur 
		moi.main_frame.setVisible(true);
		
		
		
		try {
			Socket socket = new Socket(host, port);
			// BufferedReader console = new BufferedReader(new InputStreamReader(System.in)); on utilisé avant une console mais ce n'est plus nécessaire depuis l'interface grahpique
			
			// On se connecte et on génère un canaux de lecture et un canaux d'écriture ; nos données sont des String
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
			
			// On applique certaines des fonctions aux boutons que maintenant car il nous fallait le PrintWriter vers le serveur
			moi.info_membre_group.addActionListener(e -> {moi.afficher_membre(moi.actual_chat);});
			moi.quitter_groupe.addActionListener(e -> {moi.partir_groupe(moi.actual_chat, out);});
			moi.ajouter_membre.addActionListener(e -> {moi.ajouter_membre(moi.actual_chat, out);});
			
			moi.quitter_app_btn.addActionListener(e -> {try {
				moi.deconnecter(out, socket);
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}});
			

			moi.send_message.addActionListener(e -> {
				moi.lancer_envoie_message(out);
			});

			moi.register(out, in);

			moi.add_group_btn.addActionListener(e -> {
				moi.lancerCreationGroupe(out, moi.get_other_users("global"));
			});


			Thread ecoute = new Thread(() -> { // ceci est un thread (quel commentaire remplie de sens)
				String message;
				try {
					while ((message = in.readLine()) != null) {

						moi.recevoir_message(message, out); // On recoit un message alors on le traite. Simplement


					}
				} catch (SocketException e) {
					System.out.println("je suis plus la");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			});
			ecoute.start();
			
			// On s'est dit qu'un ecouteur de clavier pour envoyer lors de l'appui de la touche entrée ne serait pas de refus. Pour éviter de créer une classe entière
			// implémentant MouseListner, on a créer une KeyAdapter où l'on Override la fonction KeyPressed
			KeyAdapter lancer_lenvoie = new KeyAdapter() {
	            @Override
	            public void keyPressed(KeyEvent e) {
	                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	                    if (!moi.chatInput.getText().isBlank()) {
	                    	moi.lancer_envoie_message(out);
	                    }
	                }
	            }
			};
			
			moi.chatInput.addKeyListener(lancer_lenvoie);
			
			// Je ne sais plus pourquoi nous avons deux Windows listener au lieu d'un mais en tout cas la deconnection reste propre quand on quitte en fermant la fenetre
			moi.main_frame.addWindowListener(new java.awt.event.WindowAdapter() {
			    @Override
			    public void windowClosing(WindowEvent e) {
			       try {
					moi.envoie_message("DISCONNECT","server","",out);
				   } catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				   }
			    }
			});
			
			
		} catch (ConnectException h) { // Quand la connexion n'a pas réussis à avoir lieu
			JOptionPane.showMessageDialog(moi.main_frame,
					"Nous sommes navré mais la connexion n'a pas pu être établie. Vérifiez votre IP ainsi que l'état du Réseau");
			moi.main_frame.dispose();
		}
		;
	}
	
	/**
	 * Vérfie qu'un texte finisse par un String msg, sans prendre en compte des ponctuations finales ou des espaces blancs
	 */
	public static boolean finitParQuoi(String msg) {
	    if (msg == null) return false;

	    
	    String sansCaracSpeciaux = msg.replaceAll("[^\\p{L}]+$", "")  // retire la ponctuation/emojis en fin de chaîne
	                          .trim();

	    String[] parts = sansCaracSpeciaux.split("\\s+"); // pour séparer par les espaces "blanc" : j'appelle espace blanc les sauts de lignes, espaces, tab...
	    if (parts.length == 0) return false;

	    String dernierMot = parts[parts.length - 1];

	    return dernierMot.equalsIgnoreCase("quoi");
	}
	

	/**
	 * Ecrit sur le PrintWriter out la commande d'envoie de message. Le message envoyé est celui contenu dans chatInput (la barre d'entrée de texte)
	 */
	private void lancer_envoie_message(PrintWriter out) {

		try {
			String message = chatInput.getText();
			String[] les_mots = message.split(" ");
			if (finitParQuoi(message)) {
				envoie_message_prank("MESSAGE_TO", actual_chat,chatInput.getText(), out);
			}
			else {
				envoie_message("MESSAGE_TO", actual_chat, chatInput.getText(), out);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		update_actual_chat();
		update_Group_Scroll_Bar();
		chatInput.setText("");
	}




	/**
	 * Met à jout le titre (notamment quand un pseudo est définit)
	 */
	public void updateTitle() {
		String title = Client.BASE_TITLE;
		if (pseudo != "" && pseudo != null) {
			title = title + " | VOTRE PSEUDO : " + (pseudo);
		}
		main_frame.setTitle(title);
	}

	/**
	 * Lance la création d'un pseudo et envoie au serveur une commande de création d'utilisateur
	 * @param out Le PrintWriter lié au Thread ClientRegistration du serveur.
	 * @param in Le BufferedReader qui lie les messages envoyé par le Thread ClientRegistration du serveur
	 */
	public void register(PrintWriter out, BufferedReader in) throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA"); // On crée un générateur de clé public/privé
		keyGen.initialize(2048); // toujours plus de sécurité
		KeyPair keyPair = keyGen.generateKeyPair(); // Pour générer le duo de clé
		// On les sépare en deux et les ajoute à l'instance de client
		this.cle_public = keyPair.getPublic();
		this.cle_prive = keyPair.getPrivate();
		
		// On demande le pseudo
		String pseudo = JOptionPane.showInputDialog(main_frame, "Choisissez votre pseudo !");
		if (pseudo.equals("")) {
			JOptionPane.showMessageDialog(main_frame, "Tu t'appeleras Gabriel alors ahah");
			pseudo = "Gabriel";
		}
		
		// Puis on envoit la requete avec ce pseudo
		this.envoie_message("CONNECT", "server",
				pseudo + SEP + Base64.getEncoder().encodeToString(this.cle_public.getEncoded()), out);
		
		// On attend le prochain message recu (in.readLine() attend de recevoir un message). Le prochain message recu sera les informations relatives à l'inscription (notamment le nouveau pseudo s'il a du etre change)
		recevoir_message(in.readLine(), out);

	}
	
	/**
	 * Créer un groupe avec un nom de groupe, et une liste de membres (leurs pseudo)
	 * @param out Le PrintWriter lié au Thread ClientRegistration du serveur

	 */
	public void creer_groupe(String nom, ArrayList<String> liste_ami, PrintWriter out) {
		try {
			String mes_amis = nom;
			for (String ami : liste_ami) {
				mes_amis += SEP + ami;
			}
			this.envoie_message("GROUP1", "server", mes_amis, out); // la création de groupe est géré en plusieurs requetes GROUP1 est la premiere
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Envoie un message au serveur, en le cryptant.
	 * @param intitule Le type de message (la categorie de la commande : CONNECT, MESSAGE_TO, GROUP1, ...)
	 * @param destinataire Le nom du groupe qui va recevoir le message
	 * @param message : le message NON CRYPTE (il sera crypté ici)
	 * @param out Le PrintWriter lié au Thread ClientRegistration du serveur

	 */
	public void envoie_message(String intitule, String destinataire, String message, PrintWriter out)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		
		if (destinataire.equals("server")) {
			
			// Pour les connections/deconnections, on ne s'embete pas à faire des encodage donc on met "server" en nom de destinaire et on envoie le message tel quel
			out.println(intitule + SEP + this.pseudo + SEP + "server" + SEP + message);
		} else {
			for (Contact mon_ami : contacts) {// recherche de mon ami
				if (mon_ami.pseudo.equals(destinataire)) {// si j'ai un ami
					try {
						Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); // Rembourrage auto avec le padding
						cipher.init(Cipher.ENCRYPT_MODE, mon_ami.la_clef); // On initalise cipher en mode CRYPTAGE avec la clé du groupe que l'on contact
						byte[] encrypte = cipher.doFinal(message.getBytes()); // le bloc de code encypté
						if (intitule.equals(null)) {
							intitule = "MESSAGE_TO"; // Si on a pas précisé la commande, on suppose que c'est un envoie de message
						}
						String message_crypte = Base64.getEncoder().encodeToString(encrypte); // Pour l'avoir proprement sous forme de String
						System.out.println("Le message envoyé cripté est :"+message_crypte); // Vous avez demandé d'avoir la version encrypté alors on l'affiche sur la console (sur l'interface ça fait un peu moche de garder les 2, mais on espere que de l'avoir sur la console vous suffit)
						
						
						out.println(intitule + SEP + this.pseudo + SEP + destinataire + SEP + message_crypte); // envoit du message
						act_msg_list().add(new JLabel("Vous : " + message)); // on ajoute une ligne de texte à l'historique du chat actuel
					} catch (IllegalBlockSizeException | BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}

		}
	}
	
	/**
	 * Envoie un message piégé. Le Client ne verra pas le message privé puisque cela rajoute à son historique personnel son message initial
	 * @param intitule Le type de message (la categorie de la commande : CONNECT, MESSAGE_TO, GROUP1, ...) (en principe ici MESSAGE_TO)
	 * @param destinataire Le nom du groupe qui va recevoir le message
	 * @param message : le message NON CRYPTE (il sera crypté ici)
	 * @param out Le PrintWriter lié au Thread ClientRegistration du serveur
	 */
	public void envoie_message_prank(String intitule, String destinataire, String message, PrintWriter out)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		
			for (Contact mon_ami : contacts) {// recherche de mon ami
				if (mon_ami.pseudo.equals(destinataire)) {// si j'ai un ami
					try {
						Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
						cipher.init(Cipher.ENCRYPT_MODE, mon_ami.la_clef);
						byte[] encrypte = cipher.doFinal(PRANK_MSG.getBytes()); // J'ai simplement remplacé message par PRANK_MSG
						if (intitule.equals(null)) {
							intitule = "MESSAGE_TO";
						}
						out.println(intitule + SEP + this.pseudo + SEP + destinataire + SEP
								+ Base64.getEncoder().encodeToString(encrypte));
						act_msg_list().add(new JLabel("Vous : " + message)); // A nous meme le message original est affiché, mais pour les autres utilisateurs, c'est le message piege qui est recu !
					} catch (IllegalBlockSizeException | BadPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

			}
	}
	
	
	/**
	 * Traite le message reçu : le décrypte, et en fonction de la commande, effectue l'action adapté
	 * @param out Le PrintWriter lié au Thread ClientRegistration du serveur
	 */

	public void recevoir_message(String message, PrintWriter out) throws Exception {
		try {
			boolean cond = true;
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			String[] decoupe = message.split(SEP, 4); // Premiere découpe. Permet de séparer chaque partie, chacune ayant son role
			switch (decoupe[0]) { // decoupe[0] = type de commande
			case "SET_PSEUDO": // définir le pseudo
				String[] pseudo_et_cle_et_histo = decoupe[3].split(SEP); // Parfois comme ici la derniere partie contient elle meme plusieurs bouts, on rapplique alors un split
				this.pseudo = pseudo_et_cle_et_histo[0];
				SecretKey clef = funcs.RSA_DECRYPT(pseudo_et_cle_et_histo[1], this.cle_prive); // On décrypte la cle du chat global envoyé par le serveur (elle nous permettra de crypter nos messages avec dans le chat global)
				this.contacts.add(new Contact("global", clef)); 
				JOptionPane.showMessageDialog(main_frame, "Votre pseudo a été définit comme étant : " + this.pseudo); // pour que l'utilisateur sache si son pseudo a été changé
				
				// On récupère les messages de l'historique
				cipher.init(Cipher.DECRYPT_MODE, clef);
				for (int i = 2; i < pseudo_et_cle_et_histo.length; i++) {
					String[] tab = pseudo_et_cle_et_histo[i].split(":", 2);
					this.historique.get("global").add(new JLabel(tab[0] + ": "
							+ new String(cipher.doFinal(Base64.getDecoder().decode(tab[1])), StandardCharsets.UTF_8)));
				}
				
				// On réupdate tout (nouveau groupe ajouté, nouveau messages ajouté, nouveau pseudo)
				update_Group_Scroll_Bar();
				updateTitle();
				update_actual_chat();
				break;
				
			case "MESSAGE_FROM": // aka je recoit un message
				// decoupe[1] -> pseudo du membre | decoupe[2] -> groupe dont le message provient
				for (Contact mon_ami : contacts) {
					if (mon_ami.pseudo.equals(decoupe[2])) { // je cherche le contact du groupe qui me l'a envoyé (pour utiliser sa clé)
						// Decrypte le message et l'affiche
						cipher.init(Cipher.DECRYPT_MODE, mon_ami.la_clef); // je lance le mode décryptage avec cette clé
						System.out.println("Le message reçu crypté est : "+decoupe[3]); // j'affiche le message crypté
						byte[] valeur = Base64.getDecoder().decode(decoupe[3]); // je convertit le message recu en byte
						String msg_final = ("> " + decoupe[1] + ": "+ new String(cipher.doFinal(valeur), StandardCharsets.UTF_8)); // je le décrypte et ajoute au debut le pseudo de la personne qui me l'envoit
						historique.get(decoupe[2]).add(new JLabel(msg_final));

						if (decoupe[2].equals(actual_chat)) {
							update_actual_chat(); // Si le groupe actuel est celui dont provient le message, on met a jour le chat en direct (sinon ca se fera en cliquant sur le bouton du dit-groupe)
						}

						cond = false;
						break;

					}
				}
				if (cond) {
					// Si le message ne provient pas d'un contact
					System.out.println("Vous n'etiez pas sensé lire ceci..."); // en pratique n'arrive pas et n'est jamais arrivé
				}
				break;
				
			case "GROUP2": // Permet au createur du groupe de savoir quel est le nom de groupe final (s'il n'a pas été modifié)
				String[] nom_et_membre_ban = decoupe[3].split(SEP, 2);
				JOptionPane.showMessageDialog(main_frame, "Votre nom de groupe est : " + nom_et_membre_ban[0]);
				break;
				
			

			case "GROUP3": // recevoir ceci crée un groupe dans vos contacts

				// création du groupe
				String[] nom_et_cle_et_membres = decoupe[3].split(SEP);

				Contact nouveau_contact = new Contact(nom_et_cle_et_membres[0], funcs.RSA_DECRYPT(nom_et_cle_et_membres[1], this.cle_prive));
				
				// Ajouter une liste de message avec en clé le nom du groupe
				this.historique.put(nom_et_cle_et_membres[0], new ArrayList<JLabel>());
				
				// je récupère tous les membres et je les ajoute au Contact crée
				String[] membres = nom_et_cle_et_membres[2].split("\\|");
				for (String membre : membres) {
					nouveau_contact.users.add(membre);
				}
				
				this.contacts.add(nouveau_contact); // j'ajoute ce contact à ma liste des contacts
        
				update_Group_Scroll_Bar(); // pour que le bouton pour s'y rendre apparaisse

				break;

			case "UPDATE_GROUP": // pour mettre a jour les membres d'un groupe
				
				String groupe = decoupe[1];
				String[] membres1 = decoupe[2].split("\\|");
				System.out.println("J'update le groupe "+groupe);
				Contact groupe_trouve;
				if ((groupe_trouve=get_group(groupe)) != null) {
					groupe_trouve.users = new ArrayList<String>(Arrays.asList(membres1));
				}
			
				break;
				
			case "HAS_LEAVED": // pour faire apparaitre un message d'adieu quand quelqu'un part
				
				String groupe_ = decoupe[1];
				String membre = decoupe[2];
				String message_adieu = decoupe[3];
				if (message_adieu.strip().isBlank()) {
					JLabel quitter = new JLabel(membre+" a quitté le groupe... sortez vos mouchoirs");
					quitter.setForeground(Color.gray);
					historique.get(decoupe[1]).add(quitter);
				}
				else {
					JLabel quitter = new JLabel(membre+" a quitté le groupe... ses derniers mots : ''"+message_adieu+"''");
					quitter.setForeground(Color.gray);
					historique.get(decoupe[1]).add(quitter);
				}
				
				
				if (groupe_.equals(actual_chat)) {
					update_actual_chat();
				}
				
				break;
			
			case "HAS_JOINED": // pour faire apparaitre un message de bienvenu quand quelqu'un rejoins
				
				String groupe__ = decoupe[1];
				String membre_ = decoupe[2];
				String message_adieu_ = decoupe[3]; // bon devrais s'appeller bienvenu
				if (message_adieu_.strip().isBlank()) {
					JLabel bienvenu = new JLabel(membre_+" a rejoins le groupe !");
					bienvenu.setForeground(Color.gray);
					ArrayList<JLabel> histo_act = historique.get(decoupe[1]);
					if (histo_act!=null) {
						histo_act.add(bienvenu);
					}

					
				}
				else {
					JLabel bienvenu = new JLabel(membre_+" a rejoins le groupe ! Sa salutation : ''"+message_adieu_+"''");
					bienvenu.setForeground(Color.gray);
					historique.get(decoupe[1]).add(bienvenu);
					ArrayList<JLabel> histo_act = historique.get(decoupe[1]);
					if (histo_act!=null) {
						histo_act.add(bienvenu);
					}
				}
				
				
				
				if (groupe__.equals(actual_chat)) {
					update_actual_chat();
				}
				
				break;
				
			default:
			
				System.out.println(message); // si ca correspond a rien on le print (ça peut aider a debugger)
				
			  break;
			

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
