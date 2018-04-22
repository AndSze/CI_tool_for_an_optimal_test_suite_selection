package knockKnock;

import java.net.*;
import java.io.*;

public class KnockKnockServer {
    public static void main(String[] args) throws IOException {
        

        int portNumber = 9876;
        Socket clientSocket = null;
        ServerSocket serverSocket = new ServerSocket(portNumber);
        
        while(true){

	        try {
	            clientSocket = serverSocket.accept();
			} catch (IOException e) {
			    System.out.println("Error: cannot accept client request. Exit program");
			    return;
			}
			try {
			    //create a new thread for each incoming message
			    new Thread( new KnockKnockHandler(clientSocket, "Multithreaded Server")).start();
			} catch (Exception e) {
			    //log exception and go on to next request.
			}
        }
    }
}
