import javax.crypto.SecretKey;

public class Contact {
	String pseudo;
	SecretKey la_clef;

	public Contact(String pseudo, SecretKey la_clef) {
		super();
		this.pseudo = pseudo;
		this.la_clef = la_clef;
	}

}
