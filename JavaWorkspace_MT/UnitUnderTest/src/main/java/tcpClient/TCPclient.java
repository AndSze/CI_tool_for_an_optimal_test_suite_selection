package tcpClient;

import java.io.IOException;
import java.net.Socket;

public class TCPclient{

	// we can have multiple clients, hence Socket and ClientManager are not a static variable and they are unique for each TCPclient
    private Socket clientSocket = null;
    private ClientManager CLIENTMANAGER = null;
    
    // default constructor 
    public TCPclient() {
    	//create the TCP socket server
		clientSocket = new Socket();
    }
    
    // overloaded constructor
    private TCPclient(Socket clientSocket, String serverHostName, int port) throws IOException{
    	
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
    	clientSocket = new Socket(serverHostName, port);
    	CLIENTMANAGER = new ClientManager();
    	System.out.println("Client ECHO Socket created on port = "+port);
    	
    	CLIENTMANAGER.initClientManager(clientSocket);
    	//clientmanager = new ClientManager(clientSocket);
    	//this.outputStream = clientmanager.getOutputStream();
    	//this.inputStream = clientmanager.getInputStream();
    	System.out.println("Client Manager created with outputsteam and input stream");
    	//setClientManager(clientmanager);
	    	
    }
	
	public TCPclient initClient(String serverHostName, int port) throws IOException{

		return (new TCPclient (clientSocket, serverHostName, port));
	}
	
	public void closeClient(Socket clientSocket, int port) throws IOException{
		
		
		if(clientSocket != null){
			
			CLIENTMANAGER.closeOutStream();
			CLIENTMANAGER.closeInStream();
			
			clientSocket.close();
			System.out.println("Socket for the client with port: "+port+" closed successfully");
			
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
			ReceivedMessage receivedMessage = CLIENTMANAGER.receiveMessage(t0, clientSocket);
	        System.out.printf("message {%s} after %d msec \n",receivedMessage.getMessage(),(receivedMessage.getTimestamp()-t0));
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
