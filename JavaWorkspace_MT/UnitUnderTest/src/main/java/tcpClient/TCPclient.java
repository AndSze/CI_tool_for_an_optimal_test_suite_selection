package tcpClient;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import messages.ClientMessage_BootUp;
import sensor.SensorImpl;

public class TCPclient implements Runnable {

	// we can have multiple clients, hence Socket and ClientManager are not a static variable and they are unique for each TCPclient
    private Socket clientSocket = null;
    private ClientManager clientManager = null;
	private boolean clientRunning = false;
	protected static ArrayList<SensorImpl> Client_Sensors_LIST;
	private int sensor_ID;
	private Thread clientThread = null;

	// default constructor 
    public TCPclient() {
    
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
    	super();
    	
    	if (Client_Sensors_LIST == null) {
    		Client_Sensors_LIST = new ArrayList<>();
    		System.out.println("[TCPclient " + getSensor_ID() +"] Client_Sensors_LIST created");
    	}
    }
    
    // overloaded constructor
    private TCPclient(int sensor_ID, String serverHostName, int port) throws IOException {
	    
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
	
	public TCPclient initClient(int sensor_ID, String serverHostName, int port) throws IOException{

		return (new TCPclient (sensor_ID, serverHostName, port));
	}
	
	public void run() {

    	// send BootUp message
	 	try {
			clientManager.sendMessage(new ClientMessage_BootUp(getSensor_ID()));
		 	System.out.println("[TCPclient " + getSensor_ID() +"]  Boot Up message send by the TCPClient - Client manager for the sensor is being launched");
		} catch (IOException IOex) {
			System.out.println("Error: The client for sensor ID: "+getSensor_ID()+" returns the IOException when attempted to send Boot Up message");
			IOex.printStackTrace();
		}
	
        try {
    		clientManager.messagesHandler(clientManager.getOutputStream(), clientManager.getInputReaderStream());
    	} catch (ClassNotFoundException CNFEx) {
	    	System.out.println("Error: The client for sensor ID: "+getSensor_ID()+" returns the ClassNotFoundException when class of a serialized object cannot be read by the ObjectInputStream.");
	    	CNFEx.printStackTrace();
	    } catch (IOException IOex) {
	    	System.out.println("Error: The client for sensor ID: "+getSensor_ID()+" returns the IOException when any of the usual Input/Output related exceptions take place, e.g. "
	    			+ "Something is wrong with a class used by serialization, Control information in the stream is inconsistent or Primitive data was found in the stream instead of objects");
	    	IOex.printStackTrace();
	    }
	}
	
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
	
	/*public void EchoMessageHandler(Socket clientSocket, String message) {
		
		// time point when the clients sends its message to the server
		long t0 = 0;
		try {
			//System.out.println("Client Manager that is processed by the TCPclient has outputsteam = "+CLIENTMANAGER.getOutputStream() +" and input stream = "+ CLIENTMANAGER.getInputStream());
			t0 = getClientManager().sendMessage(message, clientSocket);
			//Thread.sleep(1000);
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot send the following message: "+message);
	    	IOEx.printStackTrace();
		}
		try {
			ReceivedMessage receivedMessage = getClientManager().receiveMessage(t0, clientSocket);
	        System.out.printf("message {%s} after %d msec \n",receivedMessage.getMessage(),(receivedMessage.getTimestamp()-t0));
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client cannot receive srever's response for the following message sent: "+message);
	    	IOEx.printStackTrace();
	    }
		//return success;
	}*/
	
	public static synchronized ArrayList<SensorImpl> updateClientSensorList(SensorImpl sensor){
		int itemIndex = 0;
		if (Client_Sensors_LIST.size() == 0) {
			Client_Sensors_LIST.add(sensor);
		}
		else {
			for (SensorImpl s : Client_Sensors_LIST) {
				if (s.getSensorID() == sensor.getSensorID()) {
					Client_Sensors_LIST.set(itemIndex, sensor);
					//System.out.println("[TCPclient " + sensor.getSensorID() +"] updateClientSensorList() sensor instance on Client_Sensors_LIST has been updated");
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


}
