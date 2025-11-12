
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
	    public Socket socket;
	    public InputStream in;
	    public OutputStream out;
	    public SecretKey aesKey;
		String pseudo;
		String IP;

	    public ClientRegistration(String pseudo, String IP, Socket socket, Serveur serveur) throws IOException {
	        this.socket = socket;
	        this.in = socket.getInputStream();
	        this.out = socket.getOutputStream();
	        this.pseudo = pseudo;
	        this.IP = IP;
	    }
	    
	    
	    // Avec ce script on peut lire ce que le client envoit et traiter les demandes.
	    // Le serveur va donc lancer des threads de cette classe ; cette classe étant ce qui "traite" la connexion
	    
	    // A IMPLEMENTER : je rajouterais la demande d'un pseudo (faut aussi obtenir l'IP)
	    // Trouver un moyen de relier ça a la classe client
	    @Override
	    public void run() {
	        try {
	            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	            
	            out.println("Bienvenue !"); // OUT.PRINTLN = envoyer au client le message suivant
	            String message;
	            while ((message = in.readLine()) != null) {
	                System.out.println("Message reçu : " + message);
	                out.println("Reçu : " + message); // renvoie au client
	            }
	            
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	            try { socket.close(); } catch (IOException e) {}
	        }
	    }

}