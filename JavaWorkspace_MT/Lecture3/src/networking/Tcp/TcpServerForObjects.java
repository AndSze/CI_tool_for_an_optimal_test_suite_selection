package networking.Tcp;

import java.net.*;
import java.io.*;

public class TcpServerForObjects {
	public static void main(String args[]) {
		System.out.println("TCPServerOO waiting...");
		try {
			ServerSocket listenSocket = new ServerSocket(6789);
			while (true) {
				Socket clientSocket = listenSocket.accept();
				new ObjectConnection(clientSocket);
			}
		}
		catch (IOException e) {
			System.out.println("Listen socket:" + e.getMessage());
		}
	}
}

class ObjectConnection extends Thread {
	ObjectInputStream in;
	ObjectOutputStream out;
	Socket clientSocket;

	public ObjectConnection(Socket aClientSocket) {
		try {
			clientSocket = aClientSocket;
			in = new ObjectInputStream(clientSocket.getInputStream());
			out = new ObjectOutputStream(clientSocket.getOutputStream());
			System.out.println("Connection established");
			this.start();
		}
		catch (IOException e) {
			System.out.println("Connection:" + e.getMessage());
		}
	}

	public void run() {
		try { 
			Test data;
			try {
				data = (Test) in.readObject();
			  System.out.println("TCPServerOO received: " + data);
			  out.writeObject(data);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		catch (EOFException e) { System.out.println("EOF:" + e.getMessage()); }
		catch (IOException e)  { System.out.println("readline:" + e.getMessage()); }
		finally { 
			try { clientSocket.close(); } 
			catch (IOException e) {	System.out.println("close:" + e.getMessage()); }
		}
	}
}