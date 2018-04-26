package tcpServer;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPserver {
	
    //declare a TCP socket object and initialize it to null
	private ServerSocket serverSocket;
	// we can have only a single server, thus INSTANCE is a static variable
	private static TCPserver INSTANCE;
	//  determine the maximum number of threads running at the same time
	private final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
	private boolean isStopped = false;
	private Thread serverThread = null;
	
	// default constructor
	public TCPserver() {
		try {
		//create the TCP socket server
		serverSocket = new ServerSocket();
		} catch (IOException IOEx) {
			System.out.println("Error: The server socket cannot be created");
			IOEx.printStackTrace();
		}
	};
	
	 // overloaded constructor
	private TCPserver (ServerSocket serverSocket, int port) throws ClassNotFoundException{
		
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
	    try {
		    serverSocket.setReuseAddress(true);
		    serverSocket.bind(new java.net.InetSocketAddress(port));
		    System.out.println("ECHO server created and bound at port = "+port);
		    
		    startServer(serverSocket);
		    
	    } catch (RuntimeException RTEx) {
			System.out.println("Error: The server with port: "+port+" cannot process the client messages and throws the runtime exception ");
			RTEx.printStackTrace();
	    } catch (BindException BindEx) {
			System.out.println("Error: The server with port: "+port+" already exists and cannot be bound to the requested port ");
			BindEx.printStackTrace();
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
	
	public static TCPserver getInstance()
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
	
	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void closeServer(int port) {
		this.isStopped = true;
		try {
			if(serverSocket != null){
				serverSocket.close();
				System.out.println("Socket for the server with port: "+port+" is being closed successfully");
			}
		}
		catch (IOException IOEx ){
			System.out.println("Error: The server with port="+port+" cannot be closed");
			IOEx.printStackTrace();
		}
	}
	
	public void startServer(final ServerSocket serverSocket) {
		
		serverThread = new Thread(new Runnable() {
		//Runnable serverTask = new Runnable() {
	        public void run() {
	        	/*synchronized(this){
	        		Thread.currentThread();
	            }*/
	            try {
	            	Socket clientSocket = null;
				
					while(! isStopped()) {
						try {
			                //start listening to incoming client request (blocking function)
			                System.out.println("[ECHO Server] waiting for the incoming request ...");
			                clientSocket = serverSocket.accept();
			                //new Thread(new ComputeEngine(clientSocket, computeEnginesRunningID)).start();
			                //computeEnginesRunningID += 1;
						} catch (IOException IOe) {
							if(isStopped()) {
								System.out.println("Server Stopped.") ;
				                break;
							}
							serverThread.interrupt();
							throw new RuntimeException("Error accepting client connection", IOe);
							
						}
		                
		                clientProcessingPool.submit((new ComputeEngine(clientSocket)));
					}	
	            } catch (ClassNotFoundException CNFex) {
		            System.out.println("Error: when attempted to create new ComputeEngine() instance");
	            	System.out.println(CNFex.getMessage());
	            } catch (IllegalThreadStateException ITSex) {
		            System.out.println("Error: when new Thread with MessageProcessorRunnable created");
	            	System.out.println(ITSex.getMessage());
				} catch (IOException IOe) {
		            System.out.println("Error: when attempted to open input/stream for ComputeEngine()");
		            System.out.println(IOe.getMessage());
		            clientProcessingPool.shutdown();
		        }
        	}   
		});
		// create new thread for the object defined in the runnable interface and then start run() method for that object
		//Thread serverThread = new Thread(serverTask);
	    serverThread.start();
	}
	
	public ServerSocket getServerSocket() {
		return this.serverSocket;
	}
}
