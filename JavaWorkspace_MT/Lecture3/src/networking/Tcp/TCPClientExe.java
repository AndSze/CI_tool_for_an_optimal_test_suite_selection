package networking.Tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

//https://coderanch.com/t/95928/engineering/JUnit-Sockets

public class TCPClientExe {
	public static void main(String args[]){
		// arguments supply a message and a hostname
		if (args.length < 2) {
			System.out.println("Usage: TCPClient <server host name> <msg>");
			// put the below hostAddress along with the serverPort in arguments in the run configurations
			try {
				System.out.println(InetAddress.getLocalHost().getHostAddress());
				System.out.println(InetAddress.getLocalHost());
			}
			catch (IOException e) {	System.out.println("close:" + e.getMessage()); }
			finally {
				System.exit(-1);
			}
		}
		else {
			InetAddress ip;
			String hostname;
			try{
				ip = InetAddress.getLocalHost();
				hostname = ip.getHostName();
				String[] message = { InetAddress.getLocalHost().getHostAddress(),
						"Current IP address: " + ip,
						"Current hostname: " + hostname};
				
				for (int i = 0; i<3; i++) {
					
					new TcpClient(9876, message[i]);
					System.out.println( message[i]);
					TimeUnit.SECONDS.sleep(1);
				}
			}
			catch (IOException e) {	System.out.println("close:" + e.getMessage()); }
			catch (InterruptedException e) {System.out.println("close:" + e.getMessage()); }
			}
	}
}

