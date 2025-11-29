
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

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

	    public ClientRegistration(String pseudo, String IP, Socket socket, Serveur serveur) throws IOException {
	        this.socket = socket;
	        this.in = socket.getInputStream();
	        this.out = socket.getOutputStream();
	        this.pseudo = pseudo;
	        this.IP = IP;
	        this.host = serveur;
	    }
	    
	    
	    // Avec ce script on peut lire ce que le client envoit et traiter les demandes.
	    // En gros le serveur sert à faire le lien entre cette classe et la classe client de l'appareil de l'utilisateur
	    // Toutes les instances de ClientRegistration sont faite sur le PC de l'hote
	    // Le serveur va donc lancer des threads de cette classe ; cette classe étant ce qui "traite" la connexion
	    
	    // A IMPLEMENTER :
	    // je rajouterais la demande d'un pseudo (faut aussi obtenir l'IP)
	    // Quand on recevra les demandes d'envoie de message, il faudra une fonction dans la classe Serveur pour faire
	    // le partage de clés + l'envoie du message
	    // En gros ca va faire ClientRegistration.
	    @Override
	    public void run() {
	        try {
	            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				String message;
				while ((message = in.readLine()) != null) {
					// Attente que le client envoie quelque chose

					String[] message_slitted = message.split("|", 1);
					String type_message = message_slitted[0];
					if (type_message == "CONNECT") { // CONNEXT|Server|pseudo|cle_publique
						String[] new_split = message_slitted[1].split("|", 3);
						// new_split[0] est inutile ici
						String clientPseudo = new_split[1];
						String clientClePublique = new_split[2];
						this.pseudo = this.host.rendre_unique(clientPseudo);
						this.cle_public = clientClePublique;
					


						if (!this.pseudo.equals(clientPseudo)) {
							out.println("Le pseudo '" + clientPseudo + "' est déjà pris. Votre nouveau pseudo est : " + this.pseudo);
						} 
						else {
							out.println("Votre pseudo '" + this.pseudo + "' a été enregistré avec succès.");
						}

						host.groupe_general.ajouter_membre(this);
					}
					else if (type_message == "MESSAGE_TO") { // MESSAGE|destinataire|contenu_message
						// Gérer l'envoie de message
						String[] new_split = message_slitted[1].split("|", 2);
						String destinataire = new_split[0];
						String contenu_message = new_split[1];
						ClientRegistration client_destinataire = host.find_by_pseudo(destinataire);
						if (client_destinataire != null) 
						{
							PrintWriter out_destinataire = new PrintWriter(client_destinataire.socket.getOutputStream(), true);
							out_destinataire.println("MESSAGE_FROM|" + this.pseudo + "|MP|" + contenu_message);
						} 
						else 
						{
							// On cherche si c'est un groupe
							Group group_destinataire = host.find_group_by_name(destinataire);
							if (group_destinataire != null) {
								// Envoyer le message à tous les membres du groupe
								for (ClientRegistration membre : group_destinataire.get_membres()) {
									if (!membre.pseudo.equals(this.pseudo)) { // Ne pas renvoyer au sender
										PrintWriter out_membre = new PrintWriter(membre.socket.getOutputStream(), true);
											out_membre.println("MESSAGE_FROM|" + this.pseudo + "|" + group_destinataire.nom_groupe + "|" + destinataire + "|" + contenu_message);
									}
								}
							}
							else 
							{out.println("Erreur : destinataire '" + destinataire + "' non trouvé.");}
						}
					}

		

					else if (type_message == "DISCONNECT") { // DISCONNECT
						break;
						// Gérer la déconnexion
					}
				}
			
					
				
	            // Maintenant on récupère sa clé publique sans demandé à l'utilisateur 

	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            try { socket.close(); } catch (IOException e) {}
	        }
	    }

}