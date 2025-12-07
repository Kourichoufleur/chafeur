import java.util.ArrayList;

import javax.crypto.SecretKey;


/**Utilisé par Client pour stocker pour chaque groupe son nom, sa clé et sa liste de membres
 **/
public class Contact {
	String pseudo;
	SecretKey la_clef;
	ArrayList<String> users;

	
	/** Créer un groupe sans aucun membres
	 */
	public Contact(String pseudo, SecretKey la_clef) {
		super();
		this.pseudo = pseudo;
		this.la_clef = la_clef;
		this.users = new ArrayList<String>();
	}
	
	
	/** Créer un groupe avec les membres passé dans le paramètre users
	 */
	public Contact(String pseudo, SecretKey la_clef, ArrayList<String> users) {
		super();
		this.pseudo = pseudo;
		this.la_clef = la_clef;
		this.users = users;
	}
	
	/** Ajoute un membre sans créer de doublons
	 */
	public void tryAdd(String new_membre) {
		if (!users.contains(new_membre)) {
			users.add(new_membre);
		}
	}

}
