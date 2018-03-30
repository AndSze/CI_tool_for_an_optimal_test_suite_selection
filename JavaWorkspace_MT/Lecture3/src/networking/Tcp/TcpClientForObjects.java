package networking.Tcp;

import java.net.*;
import java.io.*;

public class TcpClientForObjects {
	public static void main(String args[]) {
		// arguments supply message and hostname
		if (args.length < 1) {
			System.out.println("Usage: TCPClientOO <server host name>");
			System.exit(-1);
		}
		TcpClientForObjects client = new TcpClientForObjects();
		client.session(args, new Test(1, "One"));
		client.session(args, new Test(2, "Two"));
	}
	
	/** session consists of serializing an object to the output stream
	 * and deserializing the object sent in a response 
	 * @param args - program arguments
	 * @param obj - the object to send
	 */
	void session(String args[], Object obj) {
		Socket socket = null;
		try {
			socket = new Socket(args[0], 6789);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(obj);
			try {
				Test data = (Test) in.readObject();
			  System.out.println("TCPClient received: " + data);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
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