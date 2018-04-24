package tcpClient;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;

public class TCPclient{

    private Socket clientSocket;
    private ClientManager CLIENTMANAGER;
    // we can have multiple clients, thus INSTANCE is not a static variable
    //private TCPclient INSTANCE;
    private int numberOfMsgsSent = 0;
    private int numberOfMsgsReceived = 0;
    
    // default constructor 
    public TCPclient() {
    	//create the TCP socket server
		clientSocket = new Socket();
		CLIENTMANAGER = new ClientManager();
    }
    
    // overloaded constructor
    protected TCPclient(Socket clientSocket, ClientManager CLIENTMANAGER, String serverHostName, int port) throws ClassNotFoundException{
    	
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
	    try {
	    	clientSocket = new Socket(serverHostName, port);
	    	System.out.println("Client ECHO Socket created on port = "+port);
	    	
	    	CLIENTMANAGER.initClientManager(clientSocket);
	    	//clientmanager = new ClientManager(clientSocket);
	    	//this.outputStream = clientmanager.getOutputStream();
	    	//this.inputStream = clientmanager.getInputStream();
	    	System.out.println("Client Manager created with outputsteam = "+ CLIENTMANAGER.getOutputStream() +" and input stream = "+ CLIENTMANAGER.getInputStream());
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
		try {
			if(clientSocket != null){
				clientSocket.close();
			}
		}
		catch (IOException IOEx ){
			System.out.println("Error: The client with port="+port+" cannot be closed");
			IOEx.printStackTrace();
		}
	}
	
	/*public static TCPclient getInstance() throws RemoteException
	{
	    synchronized (TCPclient.class) 
	    {
	    	if (INSTANCE == null)
	    	{
	    		INSTANCE = new TCPclient();
	    	}
	        return INSTANCE;
	    }
	}*/
	
	public void EchoMessageHandler(Socket clientSocket, String message, ClientManager clientmanager) throws InterruptedException {
		long t0, t1;
		String message_read;
		boolean success = false;
		try {
			//clientmanager.setInputStream(inputStream);
			//clientmanager.setOutputStream(outputStream);
			//System.out.println("Client Manager that is processed by the TCPclient has outputsteam = "+CLIENTMANAGER.getOutputStream() +" and input stream = "+ CLIENTMANAGER.getInputStream());
			t0 = clientmanager.sendMessage(message, clientSocket);
			//Thread.sleep(1000);
			setNumberOfMsgsSent(numberOfMsgsSent+1);
			clientmanager.receiveMessage(t0, clientSocket, numberOfMsgsSent, numberOfMsgsReceived);
			setNumberOfMsgsSent(numberOfMsgsReceived+1);

		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot send the following message: "+message);
	    	IOEx.printStackTrace();
	    }
		//return success;
	}
	
	public Socket getClientSocket() {
		return this.clientSocket;
	}

	public int getNumberOfMsgsSent() {
		return numberOfMsgsSent;
	}

	public void setNumberOfMsgsSent(int numberOfMsgsSent) {
		this.numberOfMsgsSent = numberOfMsgsSent;
	}

	public int getNumberOfMsgsReceived() {
		return numberOfMsgsReceived;
	}

	public void setNumberOfMsgsReceived(int numberOfMsgsReceived) {
		this.numberOfMsgsReceived = numberOfMsgsReceived;
	}
	
	public ClientManager getClientManager() {
		return this.CLIENTMANAGER;
	}

	public void setClientManager(ClientManager CLIENTMANAGER) {
		this.CLIENTMANAGER = CLIENTMANAGER;
	}

}
