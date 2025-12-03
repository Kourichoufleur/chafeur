import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// TOUTES LES COMMANDES DIFFERENTES :
// MESSAGE_TO | destinataire (peut etre un groupe et si null alors chat global) | contenu_message
// MESSAGE_FROM_MP|expediteur|contenu_message
// MESSAGE_FROM_GROUP|expediteur|groupe|contenu_message
// CONNECT|"Server"|pseudo|cle_publique
// DISCONNECT
public class Serveur {
	private static final String SEP = PARAMETRE.SEP;
	private ArrayList<ClientRegistration> clients_enregistres = new ArrayList<ClientRegistration>();
	private ServerSocket serveurSocket;
	private Socket clientSocket;

	private ArrayList<Group> groupes_enregistres = new ArrayList<Group>();
	Group groupe_general;

	public Serveur() throws IOException {
		serveurSocket = new ServerSocket(PARAMETRE.port);
		clientSocket = new Socket();
	}

	// La fonction qui sert a écouter les demandes de connexion et qui créer un
	// Thread avec la demande
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

		Serveur serveur = new Serveur();
		Group groupe_general = new Group("global");
		serveur.groupes_enregistres.add(groupe_general);
		serveur.groupe_general = groupe_general;

		while (true) {
			serveur.clientSocket = serveur.serveurSocket.accept();
			System.out.println("Nouveau client connecté !");

			// on lance un thread pour ce client

			// Mais ca je propose de le gérer dans la fonction run() de la classe
			// ClientRegistration
			ClientRegistration client = new ClientRegistration("", "", serveur.clientSocket, serveur);
			serveur.clients_enregistres.add(client);
			Thread client_thread = new Thread(client);

			client_thread.start();

		}

		// A coder : une fonction qui est appelé par ClientRegistration et qui demande
		// d'envoyer un message via un pseudo par exemple

		/**
		 * int port = PARAMETRE.port + 1; System.out.println("Serveur en attente sur le
		 * port " + port + "..."); SecretKey AES_key; Cipher cipher = null; try {
		 * AES_key = echanger_AES(); cipher = Cipher.getInstance("AES");
		 * cipher.init(Cipher.DECRYPT_MODE, AES_key); } catch (InvalidKeyException |
		 * NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
		 * | BadPaddingException | ClassNotFoundException | IOException e) {
		 * e.printStackTrace(); }
		 * 
		 * try (ServerSocket serveur = new ServerSocket(port)) { Socket clientSocket =
		 * serveur.accept(); System.out.println("Client connecté !"); DataInputStream in
		 * = new DataInputStream(clientSocket.getInputStream());
		 * 
		 * int messageSize; String message = null; while ((messageSize = in.readInt())
		 * != 0) { byte[] encrypte = new byte[messageSize]; in.readFully(encrypte); try
		 * { message = new String(cipher.doFinal(encrypte)); } catch
		 * (IllegalBlockSizeException | BadPaddingException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); } if (message.equalsIgnoreCase("bye")) {
		 * System.out.println("Fermeture demandée par le client..."); break; }
		 * System.out.println("Message reçu : " + message); } System.out.println("Client
		 * déconnecté."); } catch (IOException e) { e.printStackTrace(); }
		 **/
	}

	/**
	 * public static SecretKey echanger_AES() throws IOException,
	 * NoSuchAlgorithmException, ClassNotFoundException, NoSuchPaddingException,
	 * InvalidKeyException, IllegalBlockSizeException, BadPaddingException { int
	 * port = PARAMETRE.port; ServerSocket serveur = new ServerSocket(port); Socket
	 * clientSocket = serveur.accept(); ObjectOutputStream out = new
	 * ObjectOutputStream(clientSocket.getOutputStream()); ObjectInputStream in =
	 * new ObjectInputStream(clientSocket.getInputStream());
	 * 
	 * KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	 * keyGen.initialize(2048); KeyPair keyPair = keyGen.generateKeyPair();
	 * 
	 * out.writeObject(keyPair.getPublic()); out.flush(); byte[] aesChiffree =
	 * (byte[]) in.readObject(); Cipher cipherRSA = Cipher.getInstance("RSA");
	 * cipherRSA.init(Cipher.DECRYPT_MODE, keyPair.getPrivate()); byte[] aesBytes =
	 * cipherRSA.doFinal(aesChiffree);
	 * 
	 * SecretKey aesKey = new SecretKeySpec(aesBytes, "AES");
	 * System.out.println("Clé AES reçue et déchiffrée !");
	 * 
	 * return aesKey; }
	 **/

	// BASIQUEMENT CETTE FONCTION POUR L'INSTANT SE FAIT AUTOMATIQUEMENT DANS LE
	// MAIN
	/**
	 * Demande aux clients ses informations tout en conservant la connexion Recupère
	 * pseudo et IP
	 * 
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

	public ClientRegistration registerClient(Socket clientSocket)
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

		ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

		out.writeObject("register_request_pseudo");
		out.flush();

		String pseudo = (String) in.readObject();

		out.writeObject("register_request_IP");
		out.flush();

		String IP = (String) in.readObject();

		ClientRegistration newClient = new ClientRegistration(pseudo, IP, clientSocket, this);
		this.clients_enregistres.add(newClient);
		return newClient;
	}

	public boolean pseudo_is_unique(String pseudo) {
		for (ClientRegistration fiche_client : this.clients_enregistres) {
			if (fiche_client.pseudo.equals(pseudo)) {
				return false;
			}
		}
		return true;
	}

	public String rendre_unique(String pseudo_initial) {
		String pseudo_test = pseudo_initial;
		int compteur = 1;
		while (!this.pseudo_is_unique(pseudo_test)) {
			pseudo_test = pseudo_initial + "_" + compteur;
			compteur += 1;
		}
		return pseudo_test;
	}

	public Group find_group_by_name(String nom_groupe) {
		for (Group group : this.groupes_enregistres) {
			if (group.nom_groupe.equals(nom_groupe)) {
				return group;
			}
		}
		return null;
	}

	public ClientRegistration find_by_pseudo(String pseudo) {
		for (ClientRegistration fiche_client : this.clients_enregistres) {
			if (fiche_client.pseudo.equals(pseudo)) {
				return fiche_client;
			}
		}
		return null;
	}

	public ClientRegistration find_by_IP(String IP) {
		for (ClientRegistration fiche_client : this.clients_enregistres) {
			if (fiche_client.IP.equals(IP)) {
				return fiche_client;
			}
		}
		return null;
	}

	
	public boolean groupe_is_unique(String nom) {
		for (Group groupes : this.groupes_enregistres) {
			if (groupes.nom_groupe.equals(nom)) {
				return false;
			}
		}
		return true;
	}
	
	
	public void broadcast(String group, String message) {
		Group groupe = find_group_by_name(group);
		System.out.println("Je broadcast "+message+" sur "+group);
		if (groupe != null) {
			for (ClientRegistration client : groupe.membres) {
				PrintWriter p_out = new PrintWriter(client.out, true);
				p_out.println(message);
			}
		}
	}

	public String rendre_unique_groupe(String nom_initial) {
		String nom_test = nom_initial;
		int compteur = 1;
		while (!this.groupe_is_unique(nom_test)) {
			nom_test = nom_initial + "_" + compteur;
			compteur += 1;
		}
		return nom_test;
	}

	public Group creer_groupe(String nom, ArrayList<ClientRegistration> membres) throws NoSuchAlgorithmException {
		nom = rendre_unique_groupe(nom);
		Group nouveau = new Group(nom);
		for (ClientRegistration clients : membres) {
			nouveau.ajouter_membre(clients);
		}
		this.groupes_enregistres.add(nouveau);
		return nouveau;
	}
	
	public String all_pseudo(Group groupe) {
		String res = "";
		for (ClientRegistration client : groupe.membres) {
			res += client.pseudo;
		}
		if (res.length()>0) {
			res = res.substring(0, res.length()-1);
		}
		return res;
	}
	
	public void ajouter_membres_et_update(String nom_groupe, ArrayList<ClientRegistration> membres) throws Exception {
		Group le_groupe = find_group_by_name(nom_groupe);
		if (le_groupe != null) {
			for (ClientRegistration membre : membres) {
				if (!le_groupe.membres.contains(membre)) {
					le_groupe.ajouter_membre(membre);
					
				}
				
			}
			
			for (ClientRegistration membre : membres) {
				PrintWriter out_clients = new PrintWriter(membre.socket.getOutputStream(), true);
				out_clients.println("GROUP3" + SEP + "server" + SEP + SEP + le_groupe.nom_groupe + SEP
				+ funcs.RSA_ENCRYPT(le_groupe.cle_secrete, membre.cle_public)+SEP+all_pseudo(le_groupe));
				
				broadcast(nom_groupe, "HAS_JOINED"+SEP+nom_groupe+SEP+membre.pseudo+SEP+" ");
			}
			
			
			broadcast(nom_groupe, "UPDATE_GROUP"+SEP+nom_groupe+SEP+stringOfMembers(nom_groupe));
		}
	}
	
	
	public void deconnect(ClientRegistration client) {
		for (Group groupe : groupes_enregistres) {
			boolean co = groupe.membres.contains(client);
			if (co) {
				groupe.retirer_membre(client);
				broadcast(groupe.nom_groupe, "UPDATE_GROUP"+SEP+groupe.nom_groupe+SEP+stringOfMembers(groupe.nom_groupe)+SEP);
				broadcast(groupe.nom_groupe, "HAS_LEAVED"+SEP+groupe.nom_groupe+SEP+client.pseudo+SEP+" ");
			}
			
		}
	}
	
	public void deconnect(ClientRegistration client, String message_adieu) {
		for (Group groupe : groupes_enregistres) {
			boolean co = groupe.membres.contains(client);
			if (co) {
				groupe.retirer_membre(client);
				broadcast(groupe.nom_groupe, "UPDATE_GROUP"+SEP+groupe.nom_groupe+SEP+stringOfMembers(groupe.nom_groupe)+SEP);
				broadcast(groupe.nom_groupe, "HAS_LEAVED"+SEP+groupe.nom_groupe+SEP+client.pseudo+SEP+message_adieu);
			}
		}
	}
	
	
	public String stringOfMembers(String groupe) {
		String res = "";
		Group group = find_group_by_name(groupe);
		if (group != null) {
			for (ClientRegistration member : group.membres) {
				res += member.pseudo+"|";
			}
			if (res.length()>1) {
				res = res.substring(0, res.length()-1);
			}
		}
		
		return res;
		
	}

	public void retirer_personne(ClientRegistration clientRegistration, String nom_du_groupe) {
		Group le_groupe = find_group_by_name(nom_du_groupe);
		
		if (le_groupe != null) {
			le_groupe.retirer_membre(clientRegistration);
		}
	}

	/**
	 * Permet de créer une connexion client/serveur entre deux clients, et par la
	 * même occasion d'échanger leurs clés
	 * 
	 * @throws InvalidKeyException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws IOException
	 */

}
