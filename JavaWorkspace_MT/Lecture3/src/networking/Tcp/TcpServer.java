package networking.Tcp;

import java.net.*;
import java.util.Date;
import java.io.*;

public class TcpServer {
	public static void main(String args[]) {
		System.out.println("TCPServer waiting...");
		try {
			int serverPort = 9876; // the server port
			ServerSocket listenSocket = new ServerSocket(serverPort);
			while (true) {
				Socket clientSocket = listenSocket.accept();
				new DataConnection(clientSocket);
			}
		}
		catch (IOException e) {
			System.out.println("Listen socket:" + e.getMessage());
		}
	}
}

class DataConnection extends Thread {
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;

	public DataConnection(Socket aClientSocket) {
		try {
			clientSocket = aClientSocket;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			System.out.println("Connection established");
			this.start();
		}
		catch (IOException e) {
			System.out.println("Connection:" + e.getMessage());
		}
	}

	public void run() {
		try { // a time server
			String data = in.readUTF(); // read a line of data from the stream
			System.out.println("TCPServer received: " + data);
			Date date = new Date();
			out.writeUTF("It's " + date.toString());
		}
		catch (EOFException e) { System.out.println("EOF:" + e.getMessage()); }
		catch (IOException e)  { System.out.println("readline:" + e.getMessage()); }
		finally { 
			try { clientSocket.close(); } 
			catch (IOException e) {	System.out.println("close:" + e.getMessage()); }
		}
	}
}