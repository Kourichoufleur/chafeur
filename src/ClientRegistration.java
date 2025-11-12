
import java.io.*;
import java.net.*;

public class ClientRegistration implements Runnable {
    private Socket socket;

    public ClientRegistration(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            out.println("Bienvenue !");
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
