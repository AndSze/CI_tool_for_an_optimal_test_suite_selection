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
	//  determine the maximum number of threads running at the same time
	private final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
	private boolean serverRunning = false;
	private Thread serverThread = null;
	
	// default constructor
	public TCPserver() throws IOException{
		super();
	};
	
	 // overloaded constructor
	private TCPserver (int port) throws IOException{
		
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
		serverSocket = new ServerSocket();
	    serverSocket.setReuseAddress(true);
	    serverSocket.bind(new java.net.InetSocketAddress(port));
	    System.out.println("ECHO server created and bound at port = "+port);
	    
	    ServerRunning(true);
	    
	    startServer(serverSocket);

	};

	public TCPserver initServer(int port) throws IOException {
		
		return (new TCPserver(port));
	}
	
	
	synchronized boolean isServerRunning() {
		return this.serverRunning;
	}
	
	synchronized void ServerRunning(boolean isServerRunning) {
	    this.serverRunning = isServerRunning;
	}
	
	public void closeServer(TCPserver INSTANCE, int port) throws IOException{
	
		if (INSTANCE.getServerSocket()!= null){
			
			INSTANCE.getServerSocket().close();
			System.out.println("Socket for the server with port: "+port+" closed successfully");
			
			// reinitialize serverRunning to false
			this.serverRunning = false;
		} 
		else {
			throw new IllegalArgumentException();
		}
		
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
				
					System.out.println("Server Thread Started.");
					
					while(isServerRunning()) {
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
