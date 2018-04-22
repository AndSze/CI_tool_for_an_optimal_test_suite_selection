package tcpClient;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;

public class TCPclient{

    private Socket clientSocket;
    private static ClientManager clientmanager;
    private static TCPclient INSTANCE;
    
    // default constructor 
    public TCPclient() {
    	//create the TCP socket server
		clientSocket = new Socket();
    }
    
    // overloaded constructor
    protected TCPclient(Socket clientSocket, String serverHostName, int port) throws ClassNotFoundException{
    	
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
	    try {
	    	clientSocket = new Socket(serverHostName, port);
	    	System.out.println("ECHO client created on port = "+port);
	    	clientmanager = new ClientManager(clientSocket);
		    
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
			System.out.println("ECHO server created");
			new TCPclient (clientSocket, serverHostName, port);
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
	
	public static TCPclient getInstance() throws RemoteException
	{
	    synchronized (TCPclient.class) 
	    {
	    	if (INSTANCE == null)
	    	{
	    		INSTANCE = new TCPclient();
	    	}
	        return INSTANCE;
	    }
	}
	
	public void EchoMessageHandler(Socket clientSocket, String message) throws InterruptedException {
		long t0, t1;
		String message_read;
		boolean success = false;
		try {
			t0 = clientmanager.sendMessage(message);
			//Thread.sleep(2000);\
			Thread.sleep(1000);
			clientmanager.receiveMessage(t0);

		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot send the following message: "+message);
	    	IOEx.printStackTrace();
	    }
		//return success;
	}
	
	public Socket getClientSocket() {
		return this.clientSocket;
	}
	
}
