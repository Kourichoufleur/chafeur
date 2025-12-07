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



/**
 * La classe utilisé par le PC de l'hôte. S'occupe de recevoir les différentes nouvelles connexion et de leur attribuer un Thread ClientRegistration. Sert également à stocker les différents groupes existants
 * et leur utilisateurs.
 */
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

	/** La fonction qui sert a écouter les demandes de connexion et qui créer un
	/* Thread avec la demande
	 */
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

		Serveur serveur = new Serveur();
		
		// creation du groupe global (groupe publique)
		Group groupe_general = new Group("global");
		serveur.groupes_enregistres.add(groupe_general);
		serveur.groupe_general = groupe_general;

		while (true) {
			// J'attend de recevoir une nouvelle connexion
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


	}
	
	/** Vérifie qu'un pseudo entré en paramètre ne soit pas déjà présent dans la liste des clients enregistrés
	 */
	public boolean pseudo_is_unique(String pseudo) {
		for (ClientRegistration fiche_client : this.clients_enregistres) {
			if (fiche_client.pseudo.equals(pseudo)) {
				return false;
			}
		}
		return !pseudo.equals("server"); // server est utilisé pour certaines commandes donc on exclue aussi cette valeur
	}
	
	/** Génère un pseudo unique en fonction d'un pseudo supposé non unique (rajoutera _n apres le pseudo, n commencant à 1 et augmentant en fonction de la nécessité)
	 */
	public String rendre_unique(String pseudo_initial) {
		String pseudo_test = pseudo_initial;
		int compteur = 1;
		while (!this.pseudo_is_unique(pseudo_test)) {
			pseudo_test = pseudo_initial + "_" + compteur;
			compteur += 1;
		}
		return pseudo_test;
	}
	
	/** Renvoit le groupe possédant le même nom que celui passé en paramètre. Si aucun groupe ne possède ce nom, renvoit null
	 */
	public Group find_group_by_name(String nom_groupe) {
		for (Group group : this.groupes_enregistres) {
			if (group.nom_groupe.equals(nom_groupe)) {
				return group;
			}
		}
		return null;
	}
	
	/** Renvoit le ClientRegistration lié au pseudo passé en paramètre. Renvoie null si aucun client n'est trouvé
	 */
	public ClientRegistration find_by_pseudo(String pseudo) {
		for (ClientRegistration fiche_client : this.clients_enregistres) {
			if (fiche_client.pseudo.equals(pseudo)) {
				return fiche_client;
			}
		}
		return null;
	}
	
	/** Vérifie qu'un nom de groupe n'est pas déjà dans la liste des groupes créé sur le serveur
	 */
	public boolean groupe_is_unique(String nom) {
		for (Group groupes : this.groupes_enregistres) {
			if (groupes.nom_groupe.equals(nom)) {
				return false;
			}
		}
		return !nom.equals("server");
	}
	
	/** Envoit le même message à tous les membres du groupe dont le nom est passé en paramètre (s'il existe).
	 * Le message doit déjà être formaté correctement (c'est a dire COMMANDE + SEP + ...)
	 */
	public void broadcast(String group, String message) {
		Group groupe = find_group_by_name(group);
		System.out.println("Je broadcast "+message+" sur "+group);
		if (groupe != null) {
			// j'envoie le message a chaque client du groupe
			for (ClientRegistration client : groupe.membres) {
				PrintWriter p_out = new PrintWriter(client.out, true);
				p_out.println(message);
			}
		}
	}
	
	/** Génère un nom de groupe unique en fonction d'un nom supposé non unique (rajoutera _n apres le nom, n commencant à 1 et augmentant en fonction de la nécessité)
	 */
	public String rendre_unique_groupe(String nom_initial) {
		String nom_test = nom_initial;
		int compteur = 1;
		while (!this.groupe_is_unique(nom_test)) {
			nom_test = nom_initial + "_" + compteur;
			compteur += 1;
		}
		return nom_test;
	}
	
	/** A partir d'une liste de clients et d'un nom, modifie la liste des groupes pour y ajouter un nouveau groupe. Renvoit également ce-dit groupe
	 */
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
	
	/** Si nom du groupe passé en paramètre est relié à un groupe existant, ajoute chaque client passé en paramètres dans ce groupe. Cela met à jour les membres du groupe pour chaque 
	 * clients en leur envoyant un message de mise à jour du groupe
	 */
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
	
	/** Déconnecte un client. Cela va le retirer des groupes existant et envoyer une notification aux autres clients des groupes en commun + update leur liste de membres
	 */
	public void deconnect(ClientRegistration client) {
		for (Group groupe : groupes_enregistres) {
			boolean co = groupe.membres.contains(client);
			if (co) {
				groupe.retirer_membre(client);
				broadcast(groupe.nom_groupe, "UPDATE_GROUP"+SEP+groupe.nom_groupe+SEP+stringOfMembers(groupe.nom_groupe)+SEP); // pour changer les membres du groupe
				broadcast(groupe.nom_groupe, "HAS_LEAVED"+SEP+groupe.nom_groupe+SEP+client.pseudo+SEP+" "); // pour la notif
			}
			
		}
	}
	
	/** Déconnecte un client. Cela va le retirer des groupes existant et envoyer une notification aux autres clients des groupes en commun + update leur liste de membres
	 * Cette version personnalise la notification que le client quitte le serveur en mettant rajoutant message_adieu
	 */
	public void deconnect(ClientRegistration client, String message_adieu) {
		for (Group groupe : groupes_enregistres) {
			boolean co = groupe.membres.contains(client);
			if (co) {
				groupe.retirer_membre(client);
				broadcast(groupe.nom_groupe, "UPDATE_GROUP"+SEP+groupe.nom_groupe+SEP+stringOfMembers(groupe.nom_groupe)+SEP); // pour changer les membres du groupe
				broadcast(groupe.nom_groupe, "HAS_LEAVED"+SEP+groupe.nom_groupe+SEP+client.pseudo+SEP+message_adieu); // pour la notif
			}
		}
	}
	
	/** Renvoit la liste des membres d'un groupe passé en paramètre (le GROUP, PAS le nom du groupe) sous forme d'un String. Le format de ce String est "membre_1|membre_2| ... |membre_n"
	 */
	public String stringOfMembers(String groupe) {
		String res = "";
		Group group = find_group_by_name(groupe);
		if (group != null) {
			for (ClientRegistration member : group.membres) {
				res += member.pseudo+"|";
			}
			if (res.length()>1) {
				res = res.substring(0, res.length()-1); // j'enleve le dernier caractere pour match le format
			}
		}
		
		return res;
		
	}

	/** Retirer un membre d'un groupe. N'envoit aucun message particulier aux autres membres du groupe par elle-même
	 */
	public void retirer_personne(ClientRegistration clientRegistration, String nom_du_groupe) {
		Group le_groupe = find_group_by_name(nom_du_groupe);
		
		if (le_groupe != null) {
			le_groupe.retirer_membre(clientRegistration);
		}
	}


}
