
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Clients {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		InetAddress addr;
		Socket client;
	
		try{
			client = new Socket("10.157.5.106", 444);
			BufferedReader in = new BufferedReader( new InputStreamReader( client.getInputStream() ) );
			PrintWriter out = new PrintWriter(client.getOutputStream());
			
			
			
			
			Thread msg_recu = new Thread(() -> {
				String msg;
				try {
					while(!(msg = in.readLine()).equalsIgnoreCase("bye")) {
						System.out.println("Reçu: "+msg);
					}
				}catch(Exception e){
					System.out.println();
				}
			});
			msg_recu.start();
			
			Thread envoie_msg = new Thread(() -> {
				String msg;
				try {
					Scanner scan = new Scanner(System.in);
					while ( !(msg = scan.nextLine()).equalsIgnoreCase("bye")) {
						out.write( msg+"\n");
						out.flush();				
					}
				} catch (Exception e) {
					System.out.println();
				} finally {
					try {
						client.close();
						System.out.println("Déconnexion.");
						System.exit(-1);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			envoie_msg.start();
			
			
			
			
			
		}
		catch(UnknownHostException e){
			e.printStackTrace();
		}
		catch(IOException ioe){}
		
	}

}
