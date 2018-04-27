package tcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPserver {
	
    //declare a TCP socket object and initialize it to null
	private ServerSocket serverSocket;
	// we can have only a single server, thus INSTANCE is a static variable
	private static TCPserver INSTANCE = null;
	//  determine the maximum number of threads running at the same time
	private final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
	private boolean isStopped = false;
	private Thread serverThread = null;
	
	// default constructor
	public TCPserver() throws IOException{
		//create the TCP socket server
		serverSocket = new ServerSocket();
		isStopped = false;
	};
	
	 // overloaded constructor
	private TCPserver (ServerSocket serverSocket, int port) throws ClassNotFoundException, IOException{
		
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
	    super();
	    
	    serverSocket.setReuseAddress(true);
	    serverSocket.bind(new java.net.InetSocketAddress(port));
	    System.out.println("ECHO server created and bound at port = "+port);
	    
	    startServer(serverSocket);
		    
	};

	public void initServer(int port) throws IOException {
		try {
			new TCPserver (serverSocket, port);
		} catch (ClassNotFoundException CNFex) {
			//will be executed when the server cannot be created
			System.out.println("Error: Application tries to load in a TCPserver class through its string name ,but no definition for the class with the specified name could be found.");
			CNFex.printStackTrace();
		}
	}
	
	public static TCPserver getInstance() throws IOException {
	    synchronized (TCPserver.class) 
	    {
	    	if (INSTANCE == null)
	    	{
	    		INSTANCE = new TCPserver();
	    	}
	        return INSTANCE;
	    }
	}
	
	synchronized boolean isStopped() {
		return this.isStopped;
	}
	
	synchronized TCPserver tcpServerInstance() {
		return TCPserver.INSTANCE;
	}

	public synchronized void closeServer(int port) throws IOException{
		
		this.isStopped = true;
		TCPserver.INSTANCE = null;
	
		serverSocket.close();
		System.out.println("Socket for the server with port: "+port+" closed successfully");
		
	}
	
	public void startServer(final ServerSocket serverSocket){
		
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
						} catch (SocketException Sockex) {
							// this exception is beign thrown to exit the while loop and call the exception handler from startServer()
							//throw new RuntimeException("Error accepting client connection", Sockex);
							serverThread.interrupt();
							System.out.println("Server Thread Stopped.");
							break;
						} 
		                
		                clientProcessingPool.submit((new ComputeEngine(clientSocket)));
					}	
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
