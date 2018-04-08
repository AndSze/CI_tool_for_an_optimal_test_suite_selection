package networking.Tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

//https://coderanch.com/t/95928/engineering/JUnit-Sockets

public class TCPClientExe {
	public static void main(String args[]) throws IOException, InterruptedException {
		// arguments supply a message and a hostname
		if (args.length < 2) {
			System.out.println("Usage: TCPClient <server host name> <msg>");
			// put the below hostAddress along with the serverPort in arguments in the run configurations
			System.out.println(InetAddress.getLocalHost().getHostAddress());
			System.out.println(InetAddress.getLocalHost());
			System.exit(-1);
		}
		else {
			InetAddress ip;
			String hostname;
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
	}
}

