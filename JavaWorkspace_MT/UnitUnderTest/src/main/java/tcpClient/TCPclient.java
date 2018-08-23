package tcpClient;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import messages.ClientMessage_BootUp;
import sensor.SensorImpl;
import watchdog.Local_1h_Watchdog;

public class TCPclient implements Runnable {

    /***********************************************************************************************************
	 * UUT_TCPclient - Class Attributes
	 ***********************************************************************************************************/
	// there are multiple clients, hence Socket and ClientManager are not a static variable and they are unique for each TCPclient
    private Socket clientSocket = null;
    private ClientManager clientManager = null;
	private boolean clientRunning = false;
	protected static ArrayList<SensorImpl> Client_Sensors_LIST;
	private int sensor_ID;
	private Thread clientThread = null;
	
	// it is required to duplicate the below parameters here, because they are used for setting the input parameters for the local watchdog on the client side
	// measurement limit is used for determining after how many measurement datas, the measurement history request is sent - variable is set to a value received from the server in ServerMessage_SensorInfoUpdate
	protected static int measurements_limit;
	
	// watchdog scale factor is used for scaling the watchdog expiration times - variable is set to a value received from the server in ServerMessage_SensorInfoUpdate
	protected static double watchdogs_scale_factor = 0.01;

    /***********************************************************************************************************
	 * Method Name: 				public TCPclient()
	 * Description: 				UUT_TCPclient class default constructor
	 * Affected internal variables: Client_Sensors_LIST
	 * Affected external variables: Local_1h_Watchdog
	 * Called external functions: 	Local_1h_Watchdog.getInstance() 
	 ***********************************************************************************************************/
    public TCPclient() {
    
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
    	super();
    	
    	if (Client_Sensors_LIST == null) {
    		Client_Sensors_LIST = new ArrayList<>();
    		System.out.println("[TCPclient " + getSensor_ID() +"] Client_Sensors_LIST created");
    	}
    	
    	// To be Deleted
    	int temp = 0;
    	
    	// create 1h watchdog that are being checked on a regular basis - if they are about to expire, the server-client communication is being initialized. Afterward, the watchdogs are kicked and they continue to count down
        // 1h watchdog on the client side are being synchronized with the 1h watchdog on the server side in messages sent from the server
    	Local_1h_Watchdog.getInstance();
    }
    
    /***********************************************************************************************************
	 * Method Name: 				private TCPclient()
	 * Description: 				UUT_TCPclient class overloaded constructor
	 * Affected internal variables: clientSocket, sensor_ID, clientManager, clientRunning, Client_Sensors_LIST
	 * Called internal functions:	searchInClientSensorList
	 * Called external functions: 	ClientManager(), ClientManager.initClientManager(), SensorImpl()
	 ***********************************************************************************************************/
    TCPclient(int sensor_ID, String serverHostName, int port) throws IOException {
    	
	    setClientSocket(new Socket(serverHostName, port));
	    setSensor_ID(sensor_ID);
	    System.out.println("[TCPclient " + getSensor_ID() +"] Client Socket created on port = "+port);
	    
	    clientManager = new ClientManager();   	
	 	
	 	// since client managers are different objects for each TCPclient instance, all clientManager functions are called via TCPclient attribute setter (setClientManager)
    	setClientManager(clientManager.initClientManager(getClientSocket(), getSensor_ID()));
    	System.out.println("[TCPclient " + getSensor_ID() +"] Client Manager created with outputsteam and input stream");
    	clientRunning(true);
    	
    	if (searchInClientSensorList(getSensor_ID()) == null) {
		    // add the instance of sensor on the client side to the Client_Sensors_LIST
		 	Client_Sensors_LIST = updateClientSensorList(new SensorImpl(getSensor_ID()));
		 	
		 	System.out.println("[TCPclient " + getSensor_ID() +"] Client_Sensors_LIST is updated with sensor instace with ID: " + getSensor_ID());
		 	
		 	int sensor_list_counter = 1;
		 	System.out.println("[TCPclient " + getSensor_ID() +"] Client_Sensors_LIST has the following sensor instances:");
		 	for (SensorImpl sens : Client_Sensors_LIST) {
		 		System.out.println("[TCPclient " + getSensor_ID() +"]\t loop counter: " +sensor_list_counter+ "\twith the following sensor ID:\t" + sens.getSensorID());
		 		sensor_list_counter += 1;
		 	}
		}
    }
	
    /***********************************************************************************************************
	 * Method Name: 				public TCPclient initClient()
	 * Description: 				calls overloaded constructor for TCPclient						
	 * Returned value				TCPclient
	 * Called internal functions: 	TCPclient()
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	public TCPclient initClient(int sensor_ID, String serverHostName, int port) throws IOException{

		return (new TCPclient (sensor_ID, serverHostName, port));
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void run()
	 * Description: 				runnable method for TCP connection on the client side 				
	 * Called external functions: 	ClientManager.sendMessage(), ClientMessage_BootUp(), ClientManager.messagesHandler()
	 * Exceptions handled: 			IOException
	 ***********************************************************************************************************/
	public void run() {

    	// send BootUp message
	 	try {
			clientManager.sendMessage(new ClientMessage_BootUp(getSensor_ID()), clientManager.getOutputStream());
		 	System.out.println("[TCPclient " + getSensor_ID() +"]  Boot Up message send by the TCPClient - Client manager for the sensor is being launched");
		} catch (IOException IOex) {
			System.out.println("Error: The client for sensor ID: "+getSensor_ID()+" returns the IOException when attempted to send Boot Up message");
			IOex.printStackTrace();
		}
	
	 	// launch state machine for TCP connection on the client side
    	clientManager.messagesHandler(clientManager.getOutputStream(), clientManager.getInputReaderStream());
    	
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void closeClient()
	 * Description: 				closes TCP connection on the client side by closing client socket	
	 * Affected internal variables: clientSocket			
	 * Exceptions thrown: 			IOException, IllegalArgumentException
	 ***********************************************************************************************************/
	public void closeClient(TCPclient INSTANCE) throws IOException{
		
		if(INSTANCE.getClientSocket() != null){

			INSTANCE.getClientSocket().close();
			System.out.println("[TCPclient " + getSensor_ID() +"] Socket for the sensor closed successfully");
			
			// reinitialize clientRunning to false
			clientRunning(false);
		} 
		else {
			throw new IllegalArgumentException();
		}
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void closeClient()
	 * Description: 				closes client manager for TCP connection on the client side by closing client output and input streams	
	 * Affected internal variables: ClientManager.outputStream, ClientManager.inputStream, ClientManager.isClientManagerRunning			
	 * Exceptions thrown: 			IOException, IllegalArgumentException
	 ***********************************************************************************************************/
	public void closeClientManager(TCPclient INSTANCE) throws IOException{
		
		if(INSTANCE.getClientManager() != null){
				
			INSTANCE.getClientManager().closeOutStream();
			INSTANCE.getClientManager().closeInStream();
			INSTANCE.getClientManager().setClientManagerRunning(false);
			
			System.out.println("[TCPclient " + getSensor_ID() +"] ClientManager for the sensor closed successfully");
		} 
		else {
			throw new IllegalArgumentException();
		}
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public static synchronized ArrayList<SensorImpl> updateClientSensorList()
	 * Description: 				updates a sensor instance from Client_Sensors_LIST if it exists, otherwise inserts new sensor instance to Client_Sensors_LIST
	 * Affected internal variables: Client_Sensors_LIST
	 * Returned value:				Client_Sensors_LIST
	 ***********************************************************************************************************/
	public static synchronized ArrayList<SensorImpl> updateClientSensorList(SensorImpl sensor){
		int itemIndex = 0;

    	// To be Deleted
    	int temp = 0;

    	
		if (Client_Sensors_LIST.size() == 0) {
			Client_Sensors_LIST.add(sensor);
		}
		else {
			for (SensorImpl s : Client_Sensors_LIST) {
				if (s.getSensorID() == sensor.getSensorID()) {
					Client_Sensors_LIST.set(itemIndex, sensor);
					System.out.println("[TCPclient " + sensor.getSensorID() +"] sensor instance in Client_Sensors_LIST has been updated");
					break;
				} 
				else {
					itemIndex++; 
				}
			}
			if(itemIndex == (Client_Sensors_LIST.size())) {
				Client_Sensors_LIST.add(sensor);
				System.out.println("[TCPclient " + sensor.getSensorID() +"] updateClientSensorList() sensor instance has been added to Client_Sensors_LIST");
			}
		}
		return Client_Sensors_LIST;
		
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public static synchronized SensorImpl searchInClientSensorList()
	 * Description: 				searches for a sensor instance in Client_Sensors_LIST based on sensor ID
	 * Returned value:				SensorImpl
	 ***********************************************************************************************************/
	public static synchronized SensorImpl searchInClientSensorList(int sensor_ID){
		SensorImpl temp_sens = null;
		for (SensorImpl sens : Client_Sensors_LIST) {
			//System.out.println("Sensors stored in the sensors list on the client side, sensor ID: " + sens.getSensorID());
			if( sens.getSensorID() == sensor_ID) {
				temp_sens = sens;
				break;
			}
		}
		return temp_sens;
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Description: 				getters & setters for class attributes			
	 ***********************************************************************************************************/
	public synchronized Socket getClientSocket() {
		return this.clientSocket;
	}
	
	synchronized void setClientSocket(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}
	
	public synchronized ClientManager getClientManager() {
		return this.clientManager;
	}
	
	synchronized void setClientManager(ClientManager clientManager) {
		this.clientManager = clientManager;
	}
	
	public synchronized boolean isClientRunning() {
		return this.clientRunning;
	}
	
	public synchronized void clientRunning(boolean isClientRunning) {
	    this.clientRunning = isClientRunning;
	}
	
	public synchronized int getSensor_ID() {
		return sensor_ID;
	}

	public synchronized void setSensor_ID(int sensor_ID) {
		this.sensor_ID = sensor_ID;
	}
	
	
	public synchronized Thread getClientThread() {
		return clientThread;
	}

	public synchronized void setClientThread(Thread clientThread) {
		this.clientThread = clientThread;
	}

	public static int getMeasurements_limit() {
		return measurements_limit;
	}

	public static void setMeasurements_limit(int measurements_limit) {
		TCPclient.measurements_limit = measurements_limit;
	}

	public static double getWatchdogs_scale_factor() {
		return watchdogs_scale_factor;
	}

	public static void setWatchdogs_scale_factor(double watchdogs_scale_factor) {
		TCPclient.watchdogs_scale_factor = watchdogs_scale_factor;
	}


}
