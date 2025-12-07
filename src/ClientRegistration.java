
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.SecretKey;


/**Chaque client qui se connecte au serveur fait ouvrir une instance de ClientRegistration qui stock les informations du clients et gère ses requetes.
 **/
public class ClientRegistration implements Runnable {
	Socket socket;
	InputStream in;
	OutputStream out;
	SecretKey aesKey;
	String pseudo;
	String IP;
	Serveur host;
	String cle_public;
	static final String SEP = PARAMETRE.SEP;
	
	/** Créer un client avec son pseudo, son IP (pourrait servir mais n'est pas utilisé dans cette version), la socket de connexion et le serveur qui l'instancie
	 */
	public ClientRegistration(String pseudo, String IP, Socket socket, Serveur serveur) throws IOException {
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
		this.pseudo = pseudo;
		this.IP = IP; 
		this.host = serveur;
	}

	/** Avec ce script on peut lire ce que le client envoit et traiter les demandes.
	/* Toutes les instances de ClientRegistration sont faite sur le PC de l'hote
	/* Le serveur va donc lancer des threads de cette classe ; cette classe étant ce
	qui "traite" la connexion
	*/ 

	@Override
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			String message;
			while ((message = in.readLine()) != null) {
					
				// Attente que le client envoie quelque chose

				String[] message_slitted = message.split(SEP, 4); // je split le message en 4 pour traiter chaque bout differemment 
				String type_message = message_slitted[0];
				switch (type_message) { // premier bout : categorie de la requete : envoie de message, demande connection, etc.
				
				case "CONNECT": // CONNEXT|Gabriel|Server|((pseudo|cle_publique)) -> Contenu
					String[] new_split = message_slitted[3].split(SEP); // le reste des info est stocké dans message_slitted[3]
					String clientPseudo = new_split[0];
					String clientClePublique = new_split[1];
					this.pseudo = this.host.rendre_unique(clientPseudo);
					this.cle_public = clientClePublique;
					// pour que l'utilisateur qui se connecte sache son pseudo final :
					out.println("SET_PSEUDO" + SEP + "server" + SEP + SEP + this.pseudo + SEP
							+ funcs.RSA_ENCRYPT(host.groupe_general.cle_secrete, clientClePublique)
							+ host.groupe_general.histo_to_msg());// il y a déja SEP au début de histo_to_msg
           
					// je l'ajoute au serveur et envoit a tous les membres de global qu'il a rejoins le serveur
					host.groupe_general.ajouter_membre(this);
					host.broadcast("global", "UPDATE_GROUP"+SEP+"global"+SEP+host.stringOfMembers("global"));
					host.broadcast("global", "HAS_JOINED"+SEP+"global"+SEP+this.pseudo+SEP+" ");
            
					break;
					
				case "MESSAGE_TO": // MESSAGE|de_qui|destinataire|contenu_message
					// Gérer l'envoie de message
					String destinataire = message_slitted[2];
					String contenu_message = message_slitted[3];
					ClientRegistration client_destinataire = host.find_by_pseudo(destinataire);
					if (client_destinataire != null) { // Si destinataire est le pseudo de quelqu'un
						// Plus utilisé car on ne gère pas les messages privés, seulement on fait des groupes privés. Mais nous l'avons laissé au cas où on réimplémente cette 
						// Fonctionnalité
						PrintWriter out_destinataire = new PrintWriter(client_destinataire.socket.getOutputStream(),
								true);
						out_destinataire.println(
								"MESSAGE_FROM" + SEP + this.pseudo + SEP + destinataire + SEP + contenu_message);
					} else {
						// On cherche si c'est un groupe
						Group group_destinataire = host.find_group_by_name(destinataire);
						if (group_destinataire != null) {
							System.out.println(destinataire);
							group_destinataire.ajour_histo("> " + this.pseudo + ":" + contenu_message);
							// Envoyer le message à tous les membres du groupe
							for (ClientRegistration membre : group_destinataire.get_membres()) {
								if (!membre.pseudo.equals(this.pseudo)) { // Ne pas renvoyer au sender
									PrintWriter out_membre = new PrintWriter(membre.socket.getOutputStream(), true);
									out_membre.println("MESSAGE_FROM" + SEP + this.pseudo + SEP
											+ group_destinataire.nom_groupe + SEP + contenu_message);
								}
							}
						} else {
							out.println("Erreur : destinataire '" + destinataire + "' non trouvé.");
						}
					}
		
					break;
				case "GROUP1": // debut de la creation du groupe
					String[] nom_et_membres = message_slitted[3].split(SEP); // premier element : nom du groupe, les autres : nom des membres
					ArrayList<ClientRegistration> membres = new ArrayList<ClientRegistration>();
					
					String non_existant = "";
					String all_names = ""; // Stock l'ensemble des pseudos valides d'un groupe
					membres.add(this);
					all_names += this.pseudo + "|";
					ClientRegistration membre;
					for (int i = 1; i < nom_et_membres.length; i++) {
						if ((membre = host.find_by_pseudo(nom_et_membres[i])) != null) {
							membres.add(membre);
							all_names += nom_et_membres[i]+"|";
						} else {
							non_existant += SEP + membre;
						}

					}
					all_names = all_names.substring(0, all_names.length()-1); // enlever la barre | en trop
					Group nouveau = this.host.creer_groupe(nom_et_membres[0], membres);
					out.println("GROUP2" + SEP + "server" + SEP + pseudo + SEP + nouveau.nom_groupe + SEP + non_existant); // on dit au createur le nom final de son groupe
					for (ClientRegistration clients : nouveau.membres) {
						PrintWriter out_clients = new PrintWriter(clients.socket.getOutputStream(), true);
						
						out_clients.println("GROUP3" + SEP + "server" + SEP + SEP + nouveau.nom_groupe + SEP
								+ funcs.RSA_ENCRYPT(nouveau.cle_secrete, clients.cle_public)+SEP+all_names);
						// pour que chaque personne appartenant au groupe est sur leur PC le groupe qui s'ajoute + avec la liste des membres
						
					}
					
					for (ClientRegistration client : nouveau.membres) {
						host.broadcast(nouveau.nom_groupe, "HAS_JOINED"+SEP+nouveau.nom_groupe+SEP+client.pseudo+SEP+" ");
						// puis on fais apparaitre une notification pour chaque utilisateur qui rejoins
					}
					
					break;
					
				case "LEAVE_GROUP": // le client demande a quuitter un groupe
					String nom_du_groupe = message_slitted[1];
					
					try {
					host.retirer_personne(this, nom_du_groupe);
					host.broadcast(nom_du_groupe, "UPDATE_GROUP"+SEP+nom_du_groupe+SEP+host.stringOfMembers(nom_du_groupe)+SEP);
					host.broadcast(nom_du_groupe, "HAS_LEAVED"+SEP+nom_du_groupe+SEP+pseudo+SEP+"");
						}
					finally {}
					
					
					
					break;
				case "ADD_TO_GROUP" : // le client veut ajouter des membres à un groupe
					String groupe = message_slitted[1];
					String[] membres_update = message_slitted[3].split("\\|"); // pour avoir la liste des membres
					ArrayList<ClientRegistration> membres_a_updates = new ArrayList<ClientRegistration>();
					for (String membre_ : membres_update) {
						ClientRegistration membre_trouve = host.find_by_pseudo(membre_);
						if (membre_trouve != null) {
							membres_a_updates.add(membre_trouve);
						}
					}
					host.ajouter_membres_et_update(groupe, membres_a_updates); // le serveur gere le reste
					break;
				case "DISCONNECT" :
					host.deconnect(this, message_slitted[3]); // le serveur gere la deconnection
					break;
				}
			

			}
			
			// Si on recoit plus rien, l'utilisateur est considéré comme déconnecté
			host.deconnect(this);
			

		} catch (

		Exception e) {
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

}