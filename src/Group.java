import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


/**Utilisé par la classe Serveur, elle stock les informations d'un groupe. Fonctionne similairement à contact mais contient un système d'historique que le serveur peut utiliser pour rétablir des messages aux nouveaux membres
 **/
public class Group {
	String nom_groupe;
	ArrayList<ClientRegistration> membres;
	SecretKey cle_secrete;
	ArrayList<String> historique;

	public Group(String nom_groupe) throws NoSuchAlgorithmException {
		this.nom_groupe = nom_groupe;
		this.membres = new ArrayList<ClientRegistration>();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES"); // je créer une cle pour le groupe
		keyGen.init(128);
		this.cle_secrete = keyGen.generateKey();
		this.historique = new ArrayList<String>();
	}

	
	/** Ajoute un membre sans créer de doublons
	 */
	public void ajouter_membre(ClientRegistration client) {
		if (!this.membres.contains(client))   this.membres.add(client);
	}

	@SuppressWarnings("finally")
	public boolean retirer_membre(ClientRegistration client) {
		try {this.membres.remove(client); return true;}
		finally {return false;}
	}

	

	public ArrayList<ClientRegistration> get_membres() {
		return this.membres;
	}
	
	/**  Ajoute un message à l'historique. Supprime les messages troupes vieux si le nombre de message actuel atteignent PARAMETRE.taille_histo
	 */
	public void ajour_histo(String message) {
		if (this.historique.size() == PARAMETRE.taille_histo) {
			this.historique.remove(0);

		}

		this.historique.add(message);
	}
	
	
	/** Convertit l'historique de messages sauvegardés en un seul String utilisables par des fonctions tiers
	 */
	public String histo_to_msg() {
		String SEP = PARAMETRE.SEP;
		String res = SEP;
		for (String messages : this.historique) {
			res += messages + SEP;
		}
		if (res.equals(SEP)) {
			return "";
		}
		return res;
	}
}