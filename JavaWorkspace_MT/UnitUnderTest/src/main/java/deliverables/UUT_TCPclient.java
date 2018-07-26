package deliverables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import messages.SensorState;
import sensor.SensorImpl;
import tcpClient.Delays;
import tcpClient.TCPclient;
import tcpClient.Watchdog_Thresholds;
import watchdog.Local_1h_Watchdog;

public class UUT_TCPclient extends TCPclient{

    /***********************************************************************************************************
	 * UUT_TCPclient - Class Attributes
	 ***********************************************************************************************************/
    private int port;
    private int sensor_ID;
    private String serverHostName  = null;
    private TCPclient INSTANCE = null;
	/***********************************************************************************************************
   	 * Auxiliary piece of code
   	 * Specific Variable Names: 	1) int[] delays_array
	 								2) final int delays_array_size
	 								3) double[] watchdog_thresholds_array
	 								4) final int watchdog_thresholds_array_size
	 * Description: 				Interfaces for the testing purposes - to parameterize times dependencies
	 ***********************************************************************************************************/
    protected static double[] watchdog_thresholds_array = null;
    private final static int watchdog_thresholds_array_size = 4;
    protected static int[] delays_array = null;
    private final static int delays_array_size = 4;
    
    /***********************************************************************************************************
	 * Method Name: 				UUT_TCPclient()
	 * Description: 				UUT_TCPclient class default constructor
	 * Affected internal variables: port, inputStream, sensor_ID, INSTANCE, serverHostName, delays_array, watchdog_thresholds_array
	 * Affected external variables: TCPclient.sensor_ID
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
    UUT_TCPclient(int sensor_ID, int port, String serverHostName) throws IOException {
    	super();
    	this.port = port;
    	this.sensor_ID = sensor_ID;
    	this.serverHostName = serverHostName;
    	this.INSTANCE = new TCPclient();
    	this.INSTANCE.setSensor_ID(sensor_ID);
    	double temp_global_watchdog_scale_factor = 1.0;
    	delays_array = set_delays_array(temp_global_watchdog_scale_factor, delays_array_size);
    	watchdog_thresholds_array = set_watchdog_thresholds_array(temp_global_watchdog_scale_factor, watchdog_thresholds_array_size);
    }
    
    /***********************************************************************************************************
	 * Method Name: 				public static void main()
	 * Description: 				handles TCP connection with server based on a local watchdog that is synchronized with the global watchdog in messages from the TCP server sent to sensors via TCP connection
	 * Affected internal variables: delays_array, watchdog_thresholds_array
	 * Called internal functions:   UUT_TCPclient(), setINSTANCE(), runTheClient(), closeTheClient(), closeTheClientManager(), set_delays_array(), set_watchdog_thresholds_array(), processingDelay()
	 								get_watchdog_threshold(), get_delays()
	 * Called external functions:   TCPclient.searchInClientSensorList()
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
    public static void main(String []args) throws IOException{
		
		int temp_port = 9876;
		int temp_sensor_ID = 5;
		String temp_serverHostName = "localhost";
		
		// local variables
		SensorImpl UUT_sensor_instance = null;
		UUT_TCPclient uut1_TCPclient = null;
		int print_loop_count = 0;
		SensorState current_sensor_state = SensorState.DEAD;
		SensorState previous_sensor_state = SensorState.DEAD;	
		
		// create UUT_TCPclient class object
		try {
			uut1_TCPclient = new UUT_TCPclient(temp_sensor_ID, temp_port, temp_serverHostName);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP client at port: "+temp_port+" cannot be created");
			IOEx.printStackTrace();
		}
			
		// initiate TCP connection with the server to obtain configuration for the sensor
		uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
		
		// close the initial TCP connection once configuration is done
		while(true) {
			if(uut1_TCPclient.getINSTANCE().getClientThread().isAlive() != true ) {
				// close the client socket and the client manager, it will be opened again once the watchdog reaches its threshold
				uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE()));
				uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE()));
				UUT_sensor_instance = searchInClientSensorList(uut1_TCPclient.getSensor_ID());
				break;
			}
			else {
				processingDelay(100);
			}
		}
		
		// update delays_array and watchdog_thresholds_array based on sensor's getLocal_watchdog_scale_factor obtained during the configuration
		delays_array = set_delays_array(UUT_sensor_instance.getLocal_watchdog_scale_factor(), delays_array_size);
		watchdog_thresholds_array = set_watchdog_thresholds_array(UUT_sensor_instance.getLocal_watchdog_scale_factor(), watchdog_thresholds_array_size);
		
		// main control loop that initiates TCP connection if Local_1h_Watchdog reaches particular thresholds
		while (true) {
			UUT_sensor_instance = searchInClientSensorList(uut1_TCPclient.getSensor_ID());
			current_sensor_state = UUT_sensor_instance.getSensorState();
			
			if ((Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() < get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, watchdog_thresholds_array)) && (current_sensor_state == SensorState.OPERATIONAL) && (uut1_TCPclient.getINSTANCE().isClientRunning() == false)) {
				// opens the client socket activates the client manager (out/in object streams)
				System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t runTheClient() is being called when Local_1h_Watchdog equals: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]");
				uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
			}
			if ((uut1_TCPclient.getINSTANCE().getClientManager().isClientManagerRunning() == false) && (previous_sensor_state == SensorState.OPERATIONAL) && (uut1_TCPclient.getINSTANCE().isClientRunning() == true)){
				// sensors gets go to pre_operational message once it received the ack server message what means that the watchdog has been kicked
				// hence close the client socket and the client manager, it will be opened again once the watchdog reaches its threshold
				System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t closeTheClientManager() & closeTheClient() are being called when Local_1h_Watchdog equals: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]\"");
				
				// closeTheClientManager closes input/output object stremas for the ClientManager that has been already closed
				uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE()));
				uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE()));  
			}
			
			if (Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > (get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, watchdog_thresholds_array)) ) {
			
				print_loop_count++;
				if (print_loop_count == 10) {
					System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t  Local_1h_Watchdog: "+ Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
					print_loop_count = 0;
				}
				processingDelay(get_delays(Delays.MEDIUM, delays_array));
			}
			else if (Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > (get_watchdog_threshold(Watchdog_Thresholds.HIGH, watchdog_thresholds_array))) {
				print_loop_count++;
				if (print_loop_count == 5) {
					System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t  Local_1h_Watchdog: "+ Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
					print_loop_count = 0;
				}
				processingDelay(get_delays(Delays.LOW, delays_array));
			}
			else {
				processingDelay(get_delays(Delays.LOWEST, delays_array));
			}
			previous_sensor_state = searchInClientSensorList(temp_sensor_ID).getSensorState();
		}
     }
    
    /***********************************************************************************************************
	 * Method Name: 				public static TCPclient runTheClient()
	 * Description: 				initiates TCP connection with server by calling initClient() function for TCPclient INSTANCE
	 * Affected external variables: TCPclient.sensor_ID, TCPclient.clientThread
	 * Returned value				INSTANCE
	 * Called external functions:   TCPclient.initClient()
	 * Exceptions handled: 			UnknownHostException, ConnectException, IOException
	 ***********************************************************************************************************/
	public static TCPclient runTheClient(TCPclient INSTANCE, int port, String serverHostName){
		try {
			INSTANCE = INSTANCE.initClient(INSTANCE.getSensor_ID(), serverHostName, port);
			
			Thread TCPclient_thread = new Thread(INSTANCE, "TCPclient Thread");
			TCPclient_thread.start();
			INSTANCE.setClientThread(TCPclient_thread);
			
		} catch (UnknownHostException unHostEx) {
	    	System.out.println("Error: The client with port="+port+" returns the UnknownHostException if if the IP address of the host could not be determined");
	    	unHostEx.printStackTrace();
		} catch (ConnectException connectEx) {
	    	System.out.println("Error: The client with port= "+port+" returns the ConnectException while attempting to connect a socket to a remote address and port. Typically, the connection was refused remotely");
	    	connectEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The client with port="+port+" returns the IOException if the bind operation fails, or if the socket is already bound.");
	    	IOEx.printStackTrace();
	    }
		return INSTANCE;
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public static TCPclient closeTheClient()
	 * Description: 				closes TCP connection with server by calling closeClient() function for TCPclient INSTANCE
	 * Affected external variables: TCPclient.clientThread
	 * Returned value				INSTANCE
	 * Called external functions:   TCPclient.closeClient()
	 * Exceptions handled: 			IllegalArgumentException, IOException
	 ***********************************************************************************************************/
	public static TCPclient closeTheClient(TCPclient INSTANCE){	
		try {
			INSTANCE.closeClient(INSTANCE);
			
			INSTANCE.getClientThread().interrupt();
			
		} catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: The client socket for sensor ID = "+ INSTANCE.getSensor_ID() +" returns the IllegalArgumentException if there was an attempt to close a client socket that has not been initialized");
			illPTREx.printStackTrace();
		}catch (IOException IOEx ){
			System.out.println("Error: The client socket for sensor ID = "+ INSTANCE.getSensor_ID() +" cannot be closed on the client side");
			IOEx.printStackTrace();
		}
		return INSTANCE;
	}	
	
    /***********************************************************************************************************
	 * Method Name: 				public static TCPclient closeTheClientManager()
	 * Description: 				closes Client Manager for TCP connection with server by calling closeClientManager() function for TCPclient INSTANCE
	 * Returned value				INSTANCE
	 * Called external functions:   TCPclient.closeClientManager()
	 * Exceptions handled: 			IllegalArgumentException, IOException
	 ***********************************************************************************************************/
	public static TCPclient closeTheClientManager(TCPclient INSTANCE){	
		try {
			INSTANCE.closeClientManager(INSTANCE);
		} catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: The client manager for sensor ID = "+ INSTANCE.getSensor_ID() +" returns the IllegalArgumentException if there was an attempt to close a client manager that has not been initialized");
			illPTREx.printStackTrace();
		}catch (IOException IOEx ){
			System.out.println("Error: The client manager for sensor ID = "+ INSTANCE.getSensor_ID() +" cannot be closed on the client side");
			IOEx.printStackTrace();
		}
		return INSTANCE;
	}	
	
	 /***********************************************************************************************************
	 * Method Name: 				private static int[] set_delays_array()
	 * Description: 				sets delays that will be used in the runnable state machine based on watchdog_scale_factor
	 * Returned value				delays_array
	 ***********************************************************************************************************/
	protected static int[] set_delays_array(double watchdog_scale_factor, int array_size) {
		
		int[] temp_delays_array =  new int[array_size];
		
		if (watchdog_scale_factor != 1.0 ) {
			temp_delays_array[0] = (int)(1000 * watchdog_scale_factor);
			temp_delays_array[1] = (int)(10000 * watchdog_scale_factor);
			temp_delays_array[2] = (int)(100000 * watchdog_scale_factor);
			temp_delays_array[3] = (int)(1000000 * watchdog_scale_factor);
		}
		else {
			temp_delays_array[0] = 1000;
			temp_delays_array[1] = 10000;
			temp_delays_array[2] = 100000;
			temp_delays_array[3] = 1000000;
		}
		
		return temp_delays_array;	
	}
	
	 /***********************************************************************************************************
	 * Method Name: 				private static double[] set_watchdog_thresholds_array()
	 * Description: 				sets watchsdog thresholds that will be used in the runnable state machine based on watchdog_scale_factor
	 * Returned value	 			watchdog_thresholds_array
	 ***********************************************************************************************************/
	protected static double[] set_watchdog_thresholds_array(double watchdog_scale_factor, int array_size) {
		
		double[] temp_watchdog_thresholds_array =  new double[array_size];
		
		if (watchdog_scale_factor != 1.0 ) {
			temp_watchdog_thresholds_array[0] = 100 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[1] = 150 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[2] = 300 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[3] = 900 * watchdog_scale_factor;
		}
		else {
			temp_watchdog_thresholds_array[0] = 100 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[1] = 150 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[2] = 300 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[3] = 900 * watchdog_scale_factor;
		}
		
		return temp_watchdog_thresholds_array;	
	}
	
	 /***********************************************************************************************************
	 * Method Name: 				private static double get_watchdog_threshold()
	 * Description: 				retrieves watchsdog threshold based on input Watchdog_Thresholds enumeration
	 * Returned value	 			watchdog_threshold
	 ***********************************************************************************************************/
	protected static double get_watchdog_threshold(Watchdog_Thresholds THRESHOLD, double[] watchdog_thresholds_array) {
		
		double watchdog_threshold = 0;
		
		switch(THRESHOLD){
			case LOWEST: watchdog_threshold = watchdog_thresholds_array[0];
				break;
			case MEDIUM: watchdog_threshold = watchdog_thresholds_array[1];
				break;
			case HIGH: watchdog_threshold = watchdog_thresholds_array[2];
				break;
			case HIGHEST: watchdog_threshold = watchdog_thresholds_array[3];
				break;
			default:
				break;
		}

		return watchdog_threshold;
	}
	
	/***********************************************************************************************************
	* Method Name: 				private static int get_delays()
	* Description: 				retrieves delay based on input Delays enumeration
	* Returned value	 			delay
	***********************************************************************************************************/
	protected static int get_delays(Delays THRESHOLD, int[] delays_array) {
		
		int delay = 0;
		
		switch(THRESHOLD){
			case LOWEST: delay = delays_array[0];
				break;
			case LOW: delay = delays_array[1];
				break;	
			case MEDIUM: delay = delays_array[2];
				break;
			case HIGHEST: delay = delays_array[3];
				break;
			default:
				break;
		}

		return delay;
	}
	
	/***********************************************************************************************************
	* Method Name: 				private int get_delays()
	* Description: 				retrieves delay based on input Delays enumeration
	* Returned value	 			delay
	***********************************************************************************************************/
	static void processingDelay(double msec) {
	    try {
	        Thread.sleep( (int) msec);
	    } catch (InterruptedException ex) {
	        
	    }
    }
	
	/***********************************************************************************************************
	* Auxiliary piece of code
	* Description: 				getters & setters for class attributes			
	***********************************************************************************************************/
	public int getPort() {
		return this.port;
	}
	
	public String getServerHostName() {
		return this.serverHostName;
	}

	public TCPclient getINSTANCE() {
		return this.INSTANCE;
	}
	
	public void setINSTANCE(TCPclient INSTANCE) {
		this.INSTANCE = INSTANCE;
	}
	
    public int getSensor_ID() {
		return sensor_ID;
	}
}
