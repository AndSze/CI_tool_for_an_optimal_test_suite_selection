package tcpServer;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import sensor.MeasurementData;
import sensor.SensorImpl;
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
	protected static ArrayList<SensorImpl> Server_Sensors_LIST= new ArrayList<>();
	protected static ArrayList<MeasurementData[]> MeasurementHistory_LIST = new ArrayList<>();
	// reset this list after each 24 measurements
	protected static ArrayList<MeasurementData> MeasurementData_LIST= new ArrayList<>();
	protected static String Sensors_PATH = "files\\Sensors";
	private int numberOfSensors = 8;
	private float[][] sensor_coordinates_array = { {1.0f, 1.0f}, {2.0f, 1.0f}, {1.5f, 2.0f}, {2.5f, 0.5f}, {3.0f, 3.5f}, {1.0f, 3.5f}, {2.5f, 0.5f}, {0.5f, 2.5f}};
	
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
	    
	    // create instances of sensors on the server side and add them to the Server_Sensors_LIST
 		for (int i = 1; i <= numberOfSensors; i++) {	
 			Server_Sensors_LIST = updateServerSensorList(new SensorImpl(1,new Point2D.Float(sensor_coordinates_array[i-1][0],sensor_coordinates_array[i-1][1]),"Release 1"));
 		}
	    
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
	
	public static ArrayList<SensorImpl> updateServerSensorList(SensorImpl sensor){
		int itemIndex = 0;
		if (Server_Sensors_LIST.size() == 0) {
			Server_Sensors_LIST.add(sensor);
		}
		else {
			for (SensorImpl s : Server_Sensors_LIST) {
				if (s.getSensorID() == sensor.getSensorID()) {
					Server_Sensors_LIST.set(itemIndex, sensor);
					break;
				} 
				else {
					itemIndex++; 
				}
			}
			if(itemIndex == (Server_Sensors_LIST.size())) {
				Server_Sensors_LIST.add(sensor);
			}
		}
		return Server_Sensors_LIST;
		
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

	public int getNumberOfSensors() {
		return numberOfSensors;
	}

	public void setNumberOfSensors(int numberOfSensors) {
		this.numberOfSensors = numberOfSensors;
	}
	
	
}
