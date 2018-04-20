package udpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ComputeEngine implements TCPserver_interface {
	
	public ComputeEngine(ServerSocket serverSocket, boolean start) throws ClassNotFoundException {
		super();
		
		if (start == true)
		{
			while(true){
	            Socket clientSocket = null;
	            try {
	                //start listening to incoming client request (blocking function)
	                System.out.println("[ECHO Server] waiting for the incoming request ...");
	                clientSocket = serverSocket.accept();
	            } catch (IOException e) {
	                System.out.println("Error: cannot accept client request. Exit program");
	            }
	            try {
	                //create a new thread for each incoming message
	                new Thread(new TCPEchoMessagesHandler(clientSocket, "Multithreaded Server")).start();
	            } catch (Exception e) {
	                System.out.println("Error: when new Thread with MessageProcessorRunnable created");
	            }
			}
		}
	}

	public void listenForIncomingMessages() {
		// TODO Auto-generated method stub
		
	}

	public void processClinetMessage() {
		// TODO Auto-generated method stub
		
	}

	public void sendMessage() {
		// TODO Auto-generated method stub
	}		
}
