import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Group {
	String nom_groupe;
	ArrayList<ClientRegistration> membres;
	SecretKey cle_secrete;
	ArrayList<String> historique;

	public Group(String nom_groupe) throws NoSuchAlgorithmException {
		this.nom_groupe = nom_groupe;
		this.membres = new ArrayList<ClientRegistration>();
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		this.cle_secrete = keyGen.generateKey();
		this.historique = new ArrayList<String>();
	}

	public void ajouter_membre(ClientRegistration client) {
		this.membres.add(client);
	}

	public void retirer_membre(ClientRegistration client) {
		this.membres.remove(client);
	}

	public ArrayList<ClientRegistration> get_membres() {
		return this.membres;
	}

	public void ajour_histo(String message) {
		if (this.historique.size() == PARAMETRE.taille_histo) {
			this.historique.remove(0);

		}

		this.historique.add(message);
	}

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