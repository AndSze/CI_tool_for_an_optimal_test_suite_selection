package networking.Tcp;

import java.net.*;
import java.io.*;


public class TcpClient {
	public static void main(String args[]) {
		// arguments supply a message and a hostname
		if (args.length < 2) {
			System.out.println("Usage: TCPClient <server host name> <msg>");
			System.exit(-1);
		}
		Socket socket = null;
		try {
			int serverPort = 9876;
			// it is a simple socket
			// first argument is ip address, the second one is port
			socket = new Socket(args[0], serverPort);
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			args[0] = "";
			String message = String.join(" ", args);
			out.writeUTF(message); // UTF is a string encoding
			String data = in.readUTF(); // read a line of data from the stream
			System.out.println("TCPClient received: " + data);
			
			InetAddress ip;
			String hostname;
			ip = InetAddress.getLocalHost();
			hostname = ip.getHostName();
			System.out.println("Current IP address: " + ip);
			System.out.println("Current hostname: " + hostname);
		}
		catch (UnknownHostException e) { System.out.println("Socket:" + e.getMessage()); }
		catch (EOFException e) { System.out.println("EOF:" + e.getMessage()); }
		catch (IOException e) { System.out.println("readline:" + e.getMessage()); }
		finally {
			if (socket != null)
				try { socket.close(); }
				catch (IOException e) { System.out.println("close:" + e.getMessage()); }
		}
	}
}