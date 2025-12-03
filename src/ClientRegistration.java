
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

	public ClientRegistration(String pseudo, String IP, Socket socket, Serveur serveur) throws IOException {
		this.socket = socket;
		this.in = socket.getInputStream();
		this.out = socket.getOutputStream();
		this.pseudo = pseudo;
		this.IP = IP;
		this.host = serveur;
	}

	// Avec ce script on peut lire ce que le client envoit et traiter les demandes.
	// En gros le serveur sert à faire le lien entre cette classe et la classe
	// client de l'appareil de l'utilisateur
	// Toutes les instances de ClientRegistration sont faite sur le PC de l'hote
	// Le serveur va donc lancer des threads de cette classe ; cette classe étant ce
	// qui "traite" la connexion

	// A IMPLEMENTER :
	// je rajouterais la demande d'un pseudo (faut aussi obtenir l'IP)
	// Quand on recevra les demandes d'envoie de message, il faudra une fonction
	// dans la classe Serveur pour faire
	// le partage de clés + l'envoie du message
	// En gros ca va faire ClientRegistration.
	@Override
	public void run() {
		try {
			System.out.println("Client registration bien lancé !");
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			String message;
			while ((message = in.readLine()) != null) {
				System.out.println("J'ai recu " + message);
				// Attente que le client envoie quelque chose

				String[] message_slitted = message.split(SEP, 4);
				String type_message = message_slitted[0];
				System.out.println(type_message);
				System.out.println(Arrays.toString(message_slitted));
				switch (type_message) {
				case "CONNECT": // CONNEXT|Gabriel|Server|((pseudo|cle_publique)) -> Contenu
					System.out.println("Tentative de connexion");
					String[] new_split = message_slitted[3].split(SEP);
					String clientPseudo = new_split[0];
					String clientClePublique = new_split[1];
					this.pseudo = this.host.rendre_unique(clientPseudo);
					this.cle_public = clientClePublique;
					out.println("SET_PSEUDO" + SEP + "server" + SEP + SEP + this.pseudo + SEP
							+ funcs.RSA_ENCRYPT(host.groupe_general.cle_secrete, clientClePublique)
							+ host.groupe_general.histo_to_msg());// il y a déja SEP au début de histo_to_msg
           
					host.groupe_general.ajouter_membre(this);
					host.broadcast("global", "UPDATE_GROUP"+SEP+"global"+SEP+host.stringOfMembers("global"));
            
					break;
				case "MESSAGE_TO": // MESSAGE|de_qui|destinataire|contenu_message
					// Gérer l'envoie de message
					String destinataire = message_slitted[2];
					String contenu_message = message_slitted[3];
					ClientRegistration client_destinataire = host.find_by_pseudo(destinataire);
					if (client_destinataire != null) {
						// unused
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
				case "GROUP1":
					String[] nom_et_membres = message_slitted[3].split(SEP);
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
					out.println(
							"GROUP2" + SEP + "server" + SEP + pseudo + SEP + nouveau.nom_groupe + SEP + non_existant);
					for (ClientRegistration clients : membres) {
						PrintWriter out_clients = new PrintWriter(clients.socket.getOutputStream(), true);
						
						out_clients.println("GROUP3" + SEP + "server" + SEP + SEP + nouveau.nom_groupe + SEP
								+ funcs.RSA_ENCRYPT(nouveau.cle_secrete, clients.cle_public)+SEP+all_names);

					}
					
					break;
					
				case "LEAVE_GROUP":
					String nom_du_groupe = message_slitted[1];
					
					try {
					host.retirer_personne(this, nom_du_groupe);
					host.broadcast(nom_du_groupe, "UPDATE_GROUP"+SEP+nom_du_groupe+SEP+host.stringOfMembers(nom_du_groupe)+SEP);
					host.broadcast(nom_du_groupe, "HAS_LEAVED"+SEP+nom_du_groupe+SEP+pseudo);
						}
					finally {}
					
					
					
					break;
				case "ADD_TO_GROUP" :
					String groupe = message_slitted[1];
					String[] membres_update = message_slitted[3].split("\\|");
					ArrayList<ClientRegistration> membres_a_updates = new ArrayList<ClientRegistration>();
					for (String membre_ : membres_update) {
						ClientRegistration membre_trouve = host.find_by_pseudo(membre_);
						if (membre_trouve != null) {
							membres_a_updates.add(membre_trouve);
						}
					}
					host.ajouter_membres_et_update(groupe, membres_a_updates);
					break;
				case "DISCONNECT" :
					host.deconnect(this, message_slitted[3]);
					break;
				}
			

			}
			
			host.deconnect(this);
			// Maintenant on récupère sa clé publique sans demandé à l'utilisateur

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