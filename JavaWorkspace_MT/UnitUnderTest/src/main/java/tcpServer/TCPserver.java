package tcpServer;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.NotSerializableException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import sensor.MeasurementData;
import sensor.SensorImpl;
import watchdog._1h_Watchdog;
import watchdog._24h_Watchdog;

public class TCPserver {
	
    //declare a TCP socket object and initialize it to null
	private ServerSocket serverSocket = null;
	//  determine the maximum number of threads running at the same time
	//private final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
	private final ThreadPoolExecutor clientProcessingPool = new ThreadPoolExecutor(8, 8, 0L, 
			TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());
	private boolean serverRunning = false;
	
	// interface for testing purposes -> in tests it should be set to false to avoid hanging the execution in inputStream.readObject() in ComputeEngine_Runnable
	private boolean computeEngineRunning = true;
	
	private Thread serverThread = null;
	
	private static _1h_Watchdog _1hWatchdog_INSTANCE = null;
	private static _24h_Watchdog _24hWatchdog_INSTANCE = null;
	private static ComputeEngine_Processing processing_engine; 
    
	protected static ArrayList<SensorImpl> Server_Sensors_LIST= new ArrayList<>();
	protected static ArrayList<MeasurementData[]> MeasurementHistory_LIST = new ArrayList<>();

	// reset this list after each 24 measurements
	protected static ArrayList<MeasurementData> MeasurementData_LIST= new ArrayList<>();
	protected static String Sensors_PATH = "files\\Sensors";
	private int numberOfSensors = 0;
	private float[][] sensor_coordinates_array = { {1.0f, 1.0f} , {2.0f, 1.0f}, {1.5f, 2.0f}};// {2.5f, 0.5f}, {3.0f, 3.5f}, {1.0f, 3.5f}, {2.5f, 0.5f}, {0.5f, 2.5f}};

	private static boolean _1hWatchog_timestamp_table_initial[] = {false, false, false};
	//private boolean _24hWatchog_timestamp_table[] = {false, false, false};

    private static AtomicReference<boolean[]> _1hWatchog_timestamp_table = new AtomicReference<boolean[]>(_1hWatchog_timestamp_table_initial);
	private static AtomicReference<boolean[]> _24hWatchog_timestamp_table = new AtomicReference<boolean[]>();
    //_1hWatchog_timestamp_table[] = {false, false, false};

	// default constructor
	public TCPserver() throws IOException{
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
		super();
		TCPserver._1hWatchdog_INSTANCE = _1h_Watchdog.getInstance();
		TCPserver._24hWatchdog_INSTANCE = _24h_Watchdog.getInstance();
	};

	 // overloaded constructor
	private TCPserver (int port) throws IOException{
		
		// communication stuff
		serverSocket = new ServerSocket();
	    serverSocket.setReuseAddress(true);
	    serverSocket.bind(new java.net.InetSocketAddress(port));
	    System.out.println("[TCPserver] server created and bound at port = "+port);
	    
	    // set TCPserver running flag to True
	    ServerRunning(true);
	    
	    // start watchdogs
	    TCPserver._1hWatchdog_INSTANCE.setEnabled(isServerRunning());
	    TCPserver._24hWatchdog_INSTANCE.setEnabled(isServerRunning());
	    
	    // data processing stuff 
	    // create instances of sensors on the server side and add them to the Server_Sensors_LIST
	    
	    try {
	    	processing_engine = new ComputeEngine_Processing();
	    } catch (ClassNotFoundException CNFex) {
            System.out.println("Error: when new ComputeEngine failed due to class of a deserialized object cannot be found");
        	System.out.println(CNFex.getMessage());
        }
	    
	    
    	for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		try {
	    		SensorImpl temp_sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), "Release 1");
	    		processing_engine.saveSensorInfo(temp_sensor);
	    		Server_Sensors_LIST = processing_engine.updateServerSensorList(temp_sensor);
	    		
	    		set_1hWatchog_Timestamp_tableID_value(false, i-1);
	    		//setTimestamp_tableID_value(_24hWatchog_timestamp_table, false, i-1);
    		} catch (FileNotFoundException FNFex) {
                System.out.println("Error: when there was an attempt to serialize a sensor instance in the path that cannot be found by the system");
            	System.out.println(FNFex.getMessage());
            } catch (NotSerializableException NonSerex) {
                System.out.println("Error: when there was an attempt to serialize a class instance - this class instance is not serializable");
            	System.out.println(NonSerex.getMessage());
            }	
 		}
	    
    	// server starts to listen messages from sensors
	    startServer(serverSocket);

	};

	public TCPserver initServer(int port) throws IOException {
		
		return (new TCPserver(port));
	}
	
	public void closeServer(TCPserver INSTANCE, int port) throws IOException{
	
		if (INSTANCE.getServerSocket()!= null){
			
			INSTANCE.getServerSocket().close();
			System.out.println("[TCPserver] Socket for the server with port: "+port+" closed successfully");
			
			// set to 1hWatchdog 30 [s] to activate the client socket when Watchdog time before expiration reaches its specified level (client-socket opening level)
			get_1hWatchdog_INSTANCE().setTimeLeftBeforeExpiration(30);
			
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
				
					System.out.println("[TCPserver] Server Thread Started.");
					
					while(isServerRunning()) {

						try {
			                //start listening to incoming client request (blocking function)
			                System.out.println("[TCPserver] waiting for the incoming request ...");
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
						System.out.println("[TCPserver] Number of Active Threads: "+clientProcessingPool.getActiveCount());
		
		                clientProcessingPool.execute((new ComputeEngine_Runnable(clientSocket, isComputeEngineRunning())));
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
	
	public synchronized void ServerRunning(boolean isServerRunning) {
	    this.serverRunning = isServerRunning;
	}

	public int getNumberOfSensors() {
		return numberOfSensors;
	}

	public void setNumberOfSensors(int numberOfSensors) {
		this.numberOfSensors = numberOfSensors;
	}
	
	public static _1h_Watchdog get_1hWatchdog_INSTANCE() {
		return _1hWatchdog_INSTANCE;
	}

	public static void set_1hWatchdog_INSTANCE(_1h_Watchdog _1hWatchdog_INSTANCE) {
		TCPserver._1hWatchdog_INSTANCE = _1hWatchdog_INSTANCE;
	}

	public static _24h_Watchdog get_24hWatchdog_INSTANCE() {
		return _24hWatchdog_INSTANCE;
	}

	public static void set_24hWatchdog_INSTANCE(_24h_Watchdog _24hWatchdog_INSTANCE) {
		TCPserver._24hWatchdog_INSTANCE = _24hWatchdog_INSTANCE;
	}
	
	public static ComputeEngine_Processing getProcessing_engine() {
		return processing_engine;
	}
	
	public synchronized boolean isComputeEngineRunning() {
		return this.computeEngineRunning;
	}
	
	public synchronized void ComputeEngineRunning(boolean computeEngineRunning) {
	    this.computeEngineRunning = computeEngineRunning;
	}

	public static boolean areAllTrue(boolean[] array)
	{
		boolean returned_flag = false;
		int i = 0;
		for(i = 0; i < array.length; i++) {
	    	if(!array[i]) {
	    		returned_flag = false;
	    		break;
	    	}
	    	else {
	    		returned_flag = true;
	    	}
	    }
		System.out.println("[TCPserver - areAllTrue] Have all watchdogs been kicked?: " + returned_flag);
	    if (!returned_flag) System.out.println("[TCPserver - areAllTrue] If they haven't, the problematic ID is: " + i);
	    return returned_flag;
	}
	
	public static void set_1hWatchog_Allfalse()
	{
		
	    for(int i = 0; i < get_1hWatchog_timestamp_table().get().length; i++) {
	    	set_1hWatchog_Timestamp_tableID_value(false, i);
	    }
	}
	
	/*protected synchronized boolean[] get_1hWatchog_timestamp_table() {
		return _1hWatchog_timestamp_table;
	}

	protected synchronized boolean[] get_24hWatchog_timestamp_table() {
		return _24hWatchog_timestamp_table;
	}
*/
	public synchronized void set_1hWatchog_timestamp_table(boolean[] updated_array) {
		_1hWatchog_timestamp_table.set(updated_array);
	}

	public synchronized void set_24hWatchog_timestamp_table(boolean[] updated_array) {
		_24hWatchog_timestamp_table.set(updated_array);
	}

	protected synchronized static boolean get_1hWatchog_Timestamp_table_ID_value(int ID) {
		boolean[] temp = _1hWatchog_timestamp_table.get();
		return temp[ID];
	}

	protected synchronized static void set_1hWatchog_Timestamp_tableID_value(boolean new_value, int ID)  {
		boolean[] temp =  _1hWatchog_timestamp_table.get();
		temp[ID] = new_value;
		_1hWatchog_timestamp_table.set(temp);
	}
	
    public static AtomicReference<boolean[]> get_1hWatchog_timestamp_table() {
		return _1hWatchog_timestamp_table;
	}

	public static AtomicReference<boolean[]> get_24hWatchog_timestamp_table() {
		return _24hWatchog_timestamp_table;
	}


}
