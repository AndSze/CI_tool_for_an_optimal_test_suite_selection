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
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class TCPserver {
	
    /***********************************************************************************************************
	 * TCPserver - Class Attributes
	 ***********************************************************************************************************/
	
	// instance of TCPserver that should be referred in case of any modification to the TCPserver class attributes or any attempt to read the TCPserver class attribute
	private static TCPserver TCPserver_instance; 
	
    //declare a TCP socket object and initialize it to null
	private static ServerSocket serverSocket = null;
	
	//  determine the maximum number of threads running at the same time
	private final ThreadPoolExecutor serverProcessingPool = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	// instance of the Thread class that starts the server thread and enables the server to handle multiple connections with TCP clients in different threads
	private static Thread serverThread = null;
	
	// flag that indicates if TCPserver is running
	private static boolean serverRunning = false;
	
	// instance of the ComputeEngine_Processing class that triggers processing of the previously serialized files saved in the defined directory
	protected static ComputeEngine_Processing processing_engine; 
   
	// Array Lists that permanently store sensors and measurement history objects prior to save them in the defined directory
	protected static ArrayList<SensorImpl> Server_Sensors_LIST= new ArrayList<>();
	protected static ArrayList<MeasurementData[]> MeasurementHistory_LIST = new ArrayList<>();

	//  Array Lists that temporarily store sensors and measurement data objects - the list is being cleared after 24 measurements
	protected static ArrayList<MeasurementData> MeasurementData_LIST= new ArrayList<>();
	
	// directory that determines where the serialized files for a particular sensor will be saved
	protected static final String Sensors_PATH = "files\\sensors";

	// data to be loaded to sensor instances after initializing them
	protected float[][] sensor_coordinates_array = { {1.0f, 1.0f} , {2.0f, 1.0f}, {1.5f, 2.0f}};// {2.5f, 0.5f}, {3.0f, 3.5f}};//  {1.0f, 3.5f}, {2.5f, 0.5f}, {0.5f, 2.5f}};
	protected String softwareImageID = "Release 1";
	
	// initial values for the flags that indicate if the watchdogs have been kicked (it needs to be defined to have the fixed size of the flags array)
	private static boolean _1hWatchog_timestamp_table_initial[] = {false, false, false};//, false, false};
	private static boolean _24hWatchog_timestamp_table_initial[] = {false , false, false};//, false, false};

	// initialize the flags arrays that indicate if the watchdogs have been kicked - AtomicReference is being used since the flags need to be accessible in parallel in different threads
    private static AtomicReference<boolean[]> _1hWatchog_timestamp_table = new AtomicReference<boolean[]>(_1hWatchog_timestamp_table_initial);
	private static AtomicReference<boolean[]> _24hWatchog_timestamp_table = new AtomicReference<boolean[]>(_24hWatchog_timestamp_table_initial);
	
	// measurement limit is a variable that determines after how many measurement datas, the measurement history request is sent
	private static final int measurements_limit = 24;
	
	// computing time is a variable that measures duration of execution of Compute Engines Runnable thread for all sensors 
	private static double computing_time = 0;

	/***********************************************************************************************************
   	 * Auxiliary piece of code
   	 * Specific Variable Names: 	1) double watchdogs_scale_factor
	 								2) boolean set_ComputeEngineRunning
	 * Description: 				Interfaces for the testing purposes
	 ***********************************************************************************************************/
	/* watchdog scale factor is used for scaling the watchdog expiration times: 
	 * it should be used for the simulation purposes - to make the simulation of the server-client communication faster
	 * By default it should be equal 1 - what causes the following watchdog expiration times: 
	 *		1) 1hWatchog - 3600 [s]
	 *		2) 24hWatchog - 86400 [s]
	 * To accelerate the simulation, the factor has to be diminished, for example: if the factor equals 0.01, the following watchdog expiration times are defined: 
	 *  	1) 1hWatchog - 36 [s]
	 *		2) 24hWatchog - 864 [s]
	 */	
	private static double watchdogs_scale_factor = 0.01;
	
	// interface for testing purposes -> in tests it should be set to false to avoid hanging the execution in inputStream.readObject() in ComputeEngine_Runnable
	private boolean computeEngineRunning = true;
	
    /***********************************************************************************************************
	 * Method Name: 				public TCPserver()
	 * Description: 				TCPserver class default constructor
	 * Affected external variables: Global_1h_Watchdog, Global_24h_Watchdog
	 * Called external functions: 	Global_1h_Watchdog.getInstance()
	 * Exceptions thrown:			IOException
	 ***********************************************************************************************************/
	public TCPserver() throws IOException{
		// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
		super();
		
		// watchdogs that are being checked on a regular basis - if they are about to expire, the server-client communication is being initialized. Afterward, the watchdogs are kicked and they continue to count down
		Global_1h_Watchdog.getInstance();
		Global_24h_Watchdog.getInstance();
	};

    /***********************************************************************************************************
	 * Method Name: 				private TCPserver()
	 * Description: 				TCPserver class overloaded constructor
	 * Affected internal variables: serverSocket, serverRunning, processing_engine, Server_Sensors_LIST, _1hWatchog_timestamp_table, _24hWatchog_timestamp_table
	 * Affected external variables: Global_1h_Watchdog, Global_24h_Watchdog, SensorImpl
	 * Called internal functions: 	startServer()
	 * Called external functions: 	Global_1h_Watchdog.setEnabled(), Global_24h_Watchdog.setEnabled(), ComputeEngine_Processing(), SensorImpl(), ComputeEngine_Processing.saveSensorInfo(),
	 								ComputeEngine_Processing.updateServerSensorList()
	 * Exceptions thrown: 			IOException
	 * Exceptions handled: 			ClassNotFoundException, FileNotFoundException, NotSerializableException
	 ***********************************************************************************************************/
	TCPserver(int port) throws IOException{
		
		// communication stuff
		serverSocket = new ServerSocket();
	    serverSocket.setReuseAddress(true);
	    serverSocket.bind(new java.net.InetSocketAddress(port));
	    System.out.println("[TCPserver] server created and bound at port = "+port);
	    
	    // set TCPserver running flag to True
	    set_ServerRunning(true);
	    
	    // start watchdogs
	    Global_1h_Watchdog.getInstance().setEnabled(get_ServerRunning());
	    Global_24h_Watchdog.getInstance().setEnabled(get_ServerRunning());
	    Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + 
	    															 (Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() / getMeasurements_limit()) );
	    
	    // data processing stuff 
	    try {
	    	processing_engine = new ComputeEngine_Processing();
	    } catch (ClassNotFoundException CNFex) {
            System.out.println("Error: when new ComputeEngine failed due to class of a deserialized object cannot be found");
        	System.out.println(CNFex.getMessage());
        }
	    
	    // delete all files on the server side
	    processing_engine.deleteAllFilesFromDirectiory(Sensors_PATH);
	    
	    // create instances of sensors on the server side and add them to the Server_Sensors_LIST
    	for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		try {
	    		SensorImpl temp_sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, getMeasurements_limit());
	    		temp_sensor.setSensor_watchdog_scale_factor(getWatchdogs_scale_factor());
	    		Server_Sensors_LIST = processing_engine.updateServerSensorList(temp_sensor);
	    		processing_engine.saveSensorInfo(temp_sensor, "sensorINITIALIZATION");
	    		
	    		// set flags that indicate if the watchdogs have been kicked to FALSE
	    		set_1hWatchog_Timestamp_tableID_value(false, i-1);
	    		set_24hWatchog_Timestamp_tableID_value(false, i-1);
	    		
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

    /***********************************************************************************************************
	 * Method Name: 				public static synchronized TCPserver getInstance()
	 * Description: 				if this function is called for the first time, the default constructor of the TCPserver class is being called,
	 								if this function is called for the second time, the overloaded constructor of the TCPserver class is being called,
	 								otherwise, the already initialized instance of the TCPserver class is being returned
	 * Affected internal variables: TCPserver_instance								
	 * Returned value:				TCPserver_instance
	 * Called internal functions: 	TCPserver()
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
    public static synchronized TCPserver getInstance(int port) throws IOException { 
        if (TCPserver_instance == null) { 
        	TCPserver_instance = new TCPserver(); 
        } 
        else if (!get_ServerRunning()) { 
        	TCPserver_instance = new TCPserver(port); 
        } 
        return TCPserver_instance; 
    } 
 
    /***********************************************************************************************************
	 * Method Name: 				public void closeServer()
	 * Description: 				Closes server socket for TCPserver and kicks 1h_Watchdog since server socket has been closed intentionally
	 * Affected internal variables: serverSocket, serverRunning, serverThread
	 * Affected external variables: Global_1h_Watchdog.millisecondsLeftUntilExpiration
	 * Exceptions thrown: 			IOException, IllegalArgumentException
	 ***********************************************************************************************************/
	public void closeServer(TCPserver INSTANCE, int port) throws IOException{
	
		if (get_ServerRunning()){
			
			getServerSocket().close();
			getServerThread().interrupt();
			
			// set to 1hWatchdog its expiration time to activate the client socket when Watchdog time before expiration reaches its specified level (client-socket opening level)
			Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * getWatchdogs_scale_factor());
			
			// reinitialize set_ServerRunning to false
			set_ServerRunning(false);
			
			System.out.println("[TCPserver] Socket for the server with port: "+port+" closed successfully");
		} 
		else {
			throw new IllegalArgumentException();
		}
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void startServer()
	 * Description: 				Listens for connection from the client side. Handles each incoming connection in a separate thread.
	 * Affected internal variables: serverThread, serverSocket, serverProcessingPool, computing_time
	 * Called external functions: 	ComputeEngine_Runnable()
	 * Exceptions handled: 			IllegalThreadStateException, IOException
	 ***********************************************************************************************************/
	public void startServer(final ServerSocket serverSocket){
		TCPserver.serverThread = new Thread(new Runnable() {
	        public void run() {
	            try {
	            	Socket clientSocket = null;
					System.out.println("[TCPserver] Server Thread Started.");
					
					while(get_ServerRunning()) {
						try {
			                //start listening to incoming client request (blocking function)
			                System.out.println("[TCPserver] waiting for the incoming request ...");
			                clientSocket = serverSocket.accept();
						} catch (SocketException Sockex) {
							// this exception is being thrown to exit the while loop and call the exception handler from startServer()
							serverThread.interrupt();
							System.out.println("Server Thread Stopped.");
							break;
						} 
						if (serverProcessingPool.getActiveCount() == 0) {
							computing_time = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
							System.out.println("[TCPserver] 1st ComputeEngine_Runnable launched when computing_time = Global_1h_Watchdog " + Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]");
							System.out.println("[TCPserver] 1st ComputeEngine_Runnable launched when Global_24h_Watchdog " + Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]");
						}
						if (serverProcessingPool.getActiveCount() == sensor_coordinates_array.length - 1) {
							System.out.println("[TCPserver] last ComputeEngine_Runnable launched when computing_time = Global_1h_Watchdog " + Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]");
							System.out.println("[TCPserver] last ComputeEngine_Runnable launched when Global_24h_Watchdog " + Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]");
						}
						System.out.println("[TCPserver] Number of Active Threads: "+serverProcessingPool.getActiveCount());
		                serverProcessingPool.execute((new ComputeEngine_Runnable(clientSocket, getWatchdogs_scale_factor(), get_ComputeEngineRunning())));
					}	
	            } catch (IllegalThreadStateException ITSex) {
		            System.out.println("Error: when new Thread with MessageProcessorRunnable created");
	            	System.out.println(ITSex.getMessage());
				} catch (IOException IOe) {
		            System.out.println("Error: when attempted to open input/stream for ComputeEngine()");
		            System.out.println(IOe.getMessage());
		            serverProcessingPool.shutdown();
		        }
    		}   
		});
		// create new thread for the object defined in the runnable interface and then start run() method for that object
	    serverThread.start();
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Method Name: 				protected synchronized static void set_1hWatchog_Timestamp_tableID_value(boolean new_value, int ID)
	 * Description: 				Sets value of a particular element in 1hWatchog_Timestamp_tableID
	 * Affected internal variables: _1hWatchog_timestamp_table
	 ***********************************************************************************************************/
	protected synchronized static void set_1hWatchog_Timestamp_tableID_value(boolean new_value, int ID)  {
		boolean[] temp =  _1hWatchog_timestamp_table.get();
		temp[ID] = new_value;
		_1hWatchog_timestamp_table.set(temp);
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Method Name: 				protected synchronized static void set_24hWatchog_Timestamp_tableID_value(boolean new_value, int ID)
	 * Description: 				Sets value of a particular element in 24hWatchog_Timestamp_tableID
	 * Affected internal variables: _24hWatchog_timestamp_table
	 ***********************************************************************************************************/
	protected synchronized static void set_24hWatchog_Timestamp_tableID_value(boolean new_value, int ID)  {
		boolean[] temp =  _24hWatchog_timestamp_table.get();
		temp[ID] = new_value;
		_24hWatchog_timestamp_table.set(temp);
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Method Name: 				public static void set_1hWatchog_Allfalse()
	 * Description: 				Sets all elements in 1hWatchog_Timestamp_tableID to FALSE
	 * Affected internal variables: _1hWatchog_timestamp_table
	 ***********************************************************************************************************/
	public static void set_1hWatchog_Allfalse(){
		for(int i = 0; i < get_1hWatchog_timestamp_table().get().length; i++) {
	    	set_1hWatchog_Timestamp_tableID_value(false, i);
	    }
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Method Name: 				public static void set_24hWatchog_Allfalse()
	 * Description: 				Sets all elements in 24hWatchog_Timestamp_tableID to FALSE
	 * Affected internal variables: _24hWatchog_timestamp_table
	 ***********************************************************************************************************/
	public static void set_24hWatchog_Allfalse(){
		for(int i = 0; i < get_24hWatchog_timestamp_table().get().length; i++) {
	    	set_24hWatchog_Timestamp_tableID_value(false, i);
	    }
	}
	
    /***********************************************************************************************************
     * Auxiliary piece of code
	 * Method Name: 				public static boolean areAllTrue(boolean[] array)
	 * Description: 				checks if all elements in array equals true
	 * Returned value:				returned_flag
	 ***********************************************************************************************************/
	public static boolean areAllTrue(boolean[] array){
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
		/*System.out.println("[TCPserver - areAllTrue] Have all watchdogs been kicked?: " + returned_flag);
	    if (!returned_flag) System.out.println("[TCPserver - areAllTrue] If they haven't, the problematic ID is: " + i);*/
	    return returned_flag;
	}
	
    /***********************************************************************************************************
     * Auxiliary piece of code
	 * Method Name: 				public static boolean isIDTrue(boolean[] array, int ID)
	 * Description: 				checks if a particular element in array with index ID equals true
	 * Returned value:				returned_flag
	 ***********************************************************************************************************/
	public static boolean isIDTrue(boolean[] array, int ID){
		boolean returned_flag = false;
		if(array[ID-1]) {
	    	returned_flag = true;
	    }
	    return returned_flag;
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Description: 				getters & setters for class attributes			
	 ***********************************************************************************************************/
	public synchronized ServerSocket getServerSocket() {
		return serverSocket;
	}
	
	public synchronized Thread getServerThread() {
		return serverThread;
	}
	
	synchronized ThreadPoolExecutor getThreadPoolExecutor() {
		return this.serverProcessingPool;
	}	
	
	public synchronized static boolean get_ServerRunning() {
		return serverRunning;
	}
	
	public synchronized static void set_ServerRunning(boolean isServerRunning) {
	    serverRunning = isServerRunning;
	}
	
	public static ComputeEngine_Processing getProcessing_engine() {
		return processing_engine;
	}

	// auxiliary method used in set_1hWatchog_Allfalse() and areAllTrue(get_1hWatchog_timestamp_table().get())
    public static AtomicReference<boolean[]> get_1hWatchog_timestamp_table() {
		return _1hWatchog_timestamp_table;
	}

    // auxiliary method used in set_24hWatchog_Allfalse() and areAllTrue(get_24hWatchog_timestamp_table().get())
	public static AtomicReference<boolean[]> get_24hWatchog_timestamp_table() {
		return _24hWatchog_timestamp_table;
	}
	
	/******************************************************************************************************************************************
	 * Auxiliary piece of code
	 * Testing Interfaces Method Names: 1) static double getWatchdogs_scale_factor()
	 * 									2) static void setWatchdogs_scale_factor(double watchdogs_scale_factor)
	 *									3) boolean get_isComputeEngineRunning()
	 *									4) set_ComputeEngineRunning(boolean set_ComputeEngineRunning)
	 *****************************************************************************************************************************************/
	public synchronized static double getWatchdogs_scale_factor() {
		return watchdogs_scale_factor;
	}

	public synchronized static void setWatchdogs_scale_factor(double watchdogs_scale_factor) {
		TCPserver.watchdogs_scale_factor = watchdogs_scale_factor;
	}
	
	public synchronized static int getMeasurements_limit() {
		return measurements_limit;
	}
	
	public synchronized boolean get_ComputeEngineRunning() {
		return this.computeEngineRunning;
	}
	
	public synchronized void set_ComputeEngineRunning(boolean isComputeEngineRunning) {
	    this.computeEngineRunning = isComputeEngineRunning;
	}
	
    public static double getComputing_time() {
		return computing_time;
	}

	public static void setComputing_time(double computing_time) {
		TCPserver.computing_time = computing_time;
	}

}
