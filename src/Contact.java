import java.util.ArrayList;

import javax.crypto.SecretKey;

public class Contact {
	String pseudo;
	SecretKey la_clef;
	ArrayList<String> users;

	public Contact(String pseudo, SecretKey la_clef) {
		super();
		this.pseudo = pseudo;
		this.la_clef = la_clef;
		this.users = new ArrayList<String>();
	}
	public Contact(String pseudo, SecretKey la_clef, ArrayList<String> users) {
		super();
		this.pseudo = pseudo;
		this.la_clef = la_clef;
		this.users = users;
	}
	
	public void tryAdd(String new_membre) {
		if (!users.contains(new_membre)) {
			users.add(new_membre);
		}
	}

}
