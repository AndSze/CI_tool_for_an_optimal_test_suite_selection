package tcpClient;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import sensor.SensorImpl;
import watchdog.ClientWatchdog;

public class TCPclient{

	// we can have multiple clients, hence Socket and ClientManager are not a static variable and they are unique for each TCPclient
    private Socket clientSocket = null;
    private ClientManager clientManager = null;
	private boolean clientRunning = false;
	protected static ArrayList<SensorImpl> Client_Sensors_LIST= new ArrayList<>();
	
    // default constructor 
    public TCPclient() {
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
    	super();
    }
    
    // overloaded constructor
    private TCPclient(Socket clientSocket, String serverHostName, int port, ClientWatchdog serverWatchdog_INSTANCE) throws IOException{
	    
	    setClientSocket(new Socket(serverHostName, port));
	    System.out.println("Client ECHO Socket created on port = "+port);
	    
	    clientManager = new ClientManager();   	
    	setClientManager(clientManager.initClientManager(getClientSocket()));
    	
    	System.out.println("Client Manager created with outputsteam and input stream");
    	clientRunning(true);
    	serverWatchdog_INSTANCE.setEnabled(isClientRunning());
	    	
    }
	
	public TCPclient initClient(String serverHostName, int port, ClientWatchdog serverWatchdog_INSTANCE) throws IOException{

		return (new TCPclient (clientSocket, serverHostName, port, serverWatchdog_INSTANCE));
	}
	
	public void closeClient(TCPclient INSTANCE, int port) throws IOException{
		
		if(INSTANCE.getClientSocket() != null){

			INSTANCE.getClientSocket().close();
			System.out.println("Socket for the client with port: "+port+" closed successfully");
			
			// reinitialize clientRunning to false
			clientRunning(false);
		} 
		else {
			throw new IllegalArgumentException();
		}
	}
	
	public void closeClientManager(TCPclient INSTANCE, int port) throws IOException{
		
		if(INSTANCE.getClientManager() != null){
				
			INSTANCE.getClientManager().closeOutStream();
			INSTANCE.getClientManager().closeInStream();
			
			System.out.println("ClientManager for the client with port: "+port+" closed successfully");
		} 
		else {
			throw new IllegalArgumentException();
		}
	}
	
	public void EchoMessageHandler(Socket clientSocket, String message) {
		
		// time point when the clients sends its message to the server
		long t0 = 0;
		try {
			//System.out.println("Client Manager that is processed by the TCPclient has outputsteam = "+CLIENTMANAGER.getOutputStream() +" and input stream = "+ CLIENTMANAGER.getInputStream());
			t0 = getClientManager().sendMessage(message, clientSocket);
			//Thread.sleep(1000);
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot send the following message: "+message);
	    	IOEx.printStackTrace();
		}
		try {
			ReceivedMessage receivedMessage = getClientManager().receiveMessage(t0, clientSocket);
	        System.out.printf("message {%s} after %d msec \n",receivedMessage.getMessage(),(receivedMessage.getTimestamp()-t0));
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot receive srever's response for the following message sent: "+message);
	    	IOEx.printStackTrace();
	    }
		//return success;
	}
	
	public synchronized Socket getClientSocket() {
		return this.clientSocket;
	}
	
	synchronized void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}
	
	public synchronized ClientManager getClientManager() {
		return this.clientManager;
	}
	
	synchronized void setClientManager(ClientManager clientManager) {
		this.clientManager = clientManager;
	}
	
	synchronized boolean isClientRunning() {
		return this.clientRunning;
	}
	
	synchronized void clientRunning(boolean isClientRunning) {
	    this.clientRunning = isClientRunning;
	}

}
