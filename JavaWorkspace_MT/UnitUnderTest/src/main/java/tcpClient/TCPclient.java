package tcpClient;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class TCPclient{

	// we can have multiple clients, hence Socket and ClientManager are not a static variable and they are unique for each TCPclient
    private Socket clientSocket;
    private ClientManager CLIENTMANAGER;
    
    // default constructor 
    public TCPclient() {
    	//create the TCP socket server
		clientSocket = new Socket();
		CLIENTMANAGER = new ClientManager();
    }
    
    // overloaded constructor
    private TCPclient(Socket clientSocket, ClientManager CLIENTMANAGER, String serverHostName, int port) throws ClassNotFoundException{
    	
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
	    try {
	    	clientSocket = new Socket(serverHostName, port);
	    	System.out.println("Client ECHO Socket created on port = "+port);
	    	
	    	CLIENTMANAGER.initClientManager(clientSocket);
	    	//clientmanager = new ClientManager(clientSocket);
	    	//this.outputStream = clientmanager.getOutputStream();
	    	//this.inputStream = clientmanager.getInputStream();
	    	System.out.println("Client Manager created with outputsteam and input stream");
	    	//setClientManager(clientmanager);
	    	
	    } catch (SocketException socketEx) {
	    	System.out.println("Error: The client with port="+port+" returns the SocketException if there is an issue in the underlying protocol, such as a TCP error");
	    	socketEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The client with port="+port+" returns the IOException if the bind operation fails, or if the socket is already bound.");
	    	IOEx.printStackTrace();
	    }
    }
	
	public void initClient(String serverHostName, int port) {
		try {
			System.out.println("ECHO Client created");
			new TCPclient (clientSocket, CLIENTMANAGER, serverHostName, port);
		} catch (ClassNotFoundException CNFex) {
			//will be executed when the server cannot be created
			System.out.println("Error: Application tries to load in a class through its string name using "+clientSocket.getClass().getName()+" ,but no definition for the class with the specified name could be found.");
			CNFex.printStackTrace();
		}
	}
	
	public void closeClient(Socket clientSocket, int port) {
		
		if(clientSocket != null){
			CLIENTMANAGER.closeOutStream();
			try {
				CLIENTMANAGER.closeInStream();
			} catch (IOException IOEx ){
				 System.out.println("Error: when attempted to close InputStreamReader inputStream on the server side");
				IOEx.printStackTrace();
			}
			try {
				clientSocket.close();
			} catch (IOException IOEx ){
				System.out.println("Error: The client socket with port="+port+" cannot be closed on the server side");
				IOEx.printStackTrace();
			}
		}
	}
	
	public void EchoMessageHandler(Socket clientSocket, String message) {
		
		// time point when the clients sends its message to the server
		long t0 = 0;
		try {
			//System.out.println("Client Manager that is processed by the TCPclient has outputsteam = "+CLIENTMANAGER.getOutputStream() +" and input stream = "+ CLIENTMANAGER.getInputStream());
			t0 = CLIENTMANAGER.sendMessage(message, clientSocket);
			//Thread.sleep(1000);
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot send the following message: "+message);
	    	IOEx.printStackTrace();
		}
		try {
			CLIENTMANAGER.receiveMessage(t0, clientSocket);
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot receive srever's response for the following message sent: "+message);
	    	IOEx.printStackTrace();
	    }
		//return success;
	}
	
	public Socket getClientSocket() {
		return this.clientSocket;
	}

}
