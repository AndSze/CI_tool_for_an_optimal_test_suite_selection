package tcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class TCPserver {
	
    //declare a TCP socket object and initialize it to null
	private ServerSocket serverSocket;
	private static TCPserver INSTANCE;
	
	// default constructor
	public TCPserver() throws IOException {
		try {
		//create the TCP socket server
		serverSocket = new ServerSocket();
		} catch (IOException IOEx) {
			System.out.println("Error: The server socket cannot be created");
			IOEx.printStackTrace();
		}
	};
	
	 // overloaded constructor
	protected  TCPserver (ServerSocket serverSocket, int port) throws ClassNotFoundException{
		
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
	    try {
		    serverSocket.setReuseAddress(true);
		    serverSocket.bind(new java.net.InetSocketAddress(port));
		    System.out.println("ECHO server created and bound on port = "+port);
		    
		    listenThePort(serverSocket);
		    
	    } catch (SocketException socketEx) {
	    	System.out.println("Error: The server with port="+port+" returns the SocketException if there is an issue in the underlying protocol, such as a TCP error");
	    	socketEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The server with port="+port+" returns the IOException if the bind operation fails, or if the socket is already bound.");
	    	IOEx.printStackTrace();
	    }
	    
	};

	public void initServer(int port) {
		try {
			System.out.println("ECHO server created");
			new TCPserver (serverSocket, port);
		} catch (ClassNotFoundException CNFex) {
			//will be executed when the server cannot be created
			System.out.println("Error: Application tries to load in a class through its string name using "+serverSocket.getClass().getName()+" ,but no definition for the class with the specified name could be found.");
			CNFex.printStackTrace();
		}
	}
	
	public static TCPserver getInstance() throws IOException
	{
	    synchronized (TCPserver.class) 
	    {
	    	if (INSTANCE == null)
	    	{
	    		INSTANCE = new TCPserver();
	    	}
	        return INSTANCE;
	    }
	}

	public void closeServer(ServerSocket serverSocket, int port) {
		try {
			if(serverSocket != null){
				serverSocket.close();
			}
		}
		catch (IOException IOEx ){
			System.out.println("Error: The server with port="+port+" cannot be closed");
			IOEx.printStackTrace();
		}
	}
	
	public void listenThePort(ServerSocket serverSocket) {
		while(true)
		{
			Socket clientSocket = null;
			
			try {
                //start listening to incoming client request (blocking function)
                System.out.println("[ECHO Server] waiting for the incoming request ...");
                clientSocket = serverSocket.accept();
			} catch (IOException IOe) {
	            System.out.println("Error: cannot accept client request. Exit program");
	            break;
            }
            try {
                //create a new thread for each incoming message
            	//System.out.println(Thread.currentThread().getName());

            	new Thread(new ComputeEngine(clientSocket, "Multithreaded Server")).run();
            	//System.out.println(Thread.currentThread().getName());
            	
            } catch (ClassNotFoundException CNFex) {
	            System.out.println("Error: when attempted to create new ComputeEngine() instance");
            	System.out.println(CNFex.getMessage());
            	break;
            } catch (IllegalThreadStateException ITSex) {
	            System.out.println("Error: when new Thread with MessageProcessorRunnable created");
            	System.out.println(ITSex.getMessage());
	            break;
			} catch (IOException IOe) {
	            System.out.println("Error: when attempted to open input/stream for ComputeEngine()");
	            System.out.println(IOe.getMessage());
	            break;
	        }

		}
	}
}
