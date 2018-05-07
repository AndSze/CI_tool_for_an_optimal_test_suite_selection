package tcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import watchdog.ServerWatchdog;

public class TCPserver {
	
    //declare a TCP socket object and initialize it to null
	private ServerSocket serverSocket = null;
	//  determine the maximum number of threads running at the same time
	//private final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
	private final ThreadPoolExecutor clientProcessingPool = new ThreadPoolExecutor(8, 8, 0L, 
			TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
	private boolean serverRunning = false;
	private Thread serverThread = null;
	
	// default constructor
	public TCPserver() throws IOException{
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
		super();
	};
	
	 // overloaded constructor
	private TCPserver (int port, ServerWatchdog serverWatchdog_INSTANCE) throws IOException{
		
		serverSocket = new ServerSocket();
	    serverSocket.setReuseAddress(true);
	    serverSocket.bind(new java.net.InetSocketAddress(port));
	    System.out.println("ECHO server created and bound at port = "+port);
	    
	    ServerRunning(true);
	    serverWatchdog_INSTANCE.setEnabled(isServerRunning());
	    
	    startServer(serverSocket);

	};

	public TCPserver initServer(int port, ServerWatchdog serverWatchdog_INSTANCE) throws IOException {
		
		return (new TCPserver(port, serverWatchdog_INSTANCE));
	}
	
	public void closeServer(TCPserver INSTANCE, int port) throws IOException{
	
		if (INSTANCE.getServerSocket()!= null){
			
			INSTANCE.getServerSocket().close();
			System.out.println("Socket for the server with port: "+port+" closed successfully");
			
			// reinitialize serverRunning to false
			ServerRunning(false);
		} 
		else {
			throw new IllegalArgumentException();
		}
		
	}
	
	public void startServer(final ServerSocket serverSocket){
		
		this.serverThread = new Thread(new Runnable() {
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
							// this exception is being thrown to exit the while loop and call the exception handler from startServer()
							//throw new RuntimeException("Error accepting client connection", Sockex);
							serverThread.interrupt();
							System.out.println("Server Thread Stopped.");
							break;
						} 
						System.out.println("Number of Active Threads: "+clientProcessingPool.getActiveCount());
		                clientProcessingPool.execute((new ComputeEngine(clientSocket)));
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
	
	public synchronized ServerSocket getServerSocket() {
		return this.serverSocket;
	}
	
	public synchronized Thread getServerThread() {
		return this.serverThread;
	}
	
	synchronized ThreadPoolExecutor getThreadPoolExecutor() {
		return this.clientProcessingPool;
	}
	
	
	public synchronized boolean isServerRunning() {
		return this.serverRunning;
	}
	
	synchronized void ServerRunning(boolean isServerRunning) {
	    this.serverRunning = isServerRunning;
	}
	
	
}
