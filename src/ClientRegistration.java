import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.crypto.SecretKey;

public class ClientRegistration {
	    public Socket socket;
	    public ObjectInputStream in;
	    public ObjectOutputStream out;
	    public SecretKey aesKey;

	    public ClientRegistration(Socket socket, ObjectInputStream in, ObjectOutputStream out, SecretKey aesKey) {
	        this.socket = socket;
	        this.in = in;
	        this.out = out;
	        this.aesKey = aesKey;
	    }

}
