package tcpClient;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import messages.ClientMessage_BootUp;
import sensor.SensorImpl;

public class TCPclient{

	// we can have multiple clients, hence Socket and ClientManager are not a static variable and they are unique for each TCPclient
    private Socket clientSocket = null;
    private ClientManager clientManager = null;
	private boolean clientRunning = false;
	protected static ArrayList<SensorImpl> Client_Sensors_LIST;
	private int sensor_ID;

	// default constructor 
    public TCPclient() {
    
    	// if there will be any class attribute initialized to default value in the declaration section, here its value will be reinitialized
    	super();
    	
    	if (Client_Sensors_LIST == null) {
    		Client_Sensors_LIST = new ArrayList<>();
    	}
    }
    
    // overloaded constructor
    private TCPclient(int sensor_ID, String serverHostName, int port) throws IOException {
	    
	    setClientSocket(new Socket(serverHostName, port));
	    setSensor_ID(sensor_ID);
	    System.out.println("Client ECHO Socket created on port = "+port);
	    
	    clientManager = new ClientManager();   	
	 	
	 	// since client managers are different objects for each TCPclient instance, all clientManager functions are called via TCPclient attribute setter (setClientManager)
    	setClientManager(clientManager.initClientManager(getClientSocket(), getSensor_ID()));
    	System.out.println("Client Manager created with outputsteam and input stream");
    	clientRunning(true);
    	
    	// send BootUp message
    	if (searchInClientSensorList(getSensor_ID()) == null) {
    	
		    // add the instance of sensor on the client side to the Client_Sensors_LIST
		 	Client_Sensors_LIST = updateClientSensorList(new SensorImpl(getSensor_ID()));
    	}
    	
	 	clientManager.sendMessage(new ClientMessage_BootUp(getSensor_ID()));
	 	System.out.println("Boot Up message send by the Client");
    	
    	try {
    		clientManager.messagesHandler(clientManager.getOutputStream(), clientManager.getInputReaderStream());
    	} catch (ClassNotFoundException CNFEx) {
	    	System.out.println("Error: The client for sensor ID: "+getSensor_ID()+" returns the ClassNotFoundException when class of a serialized object cannot be read by the ObjectInputStream.");
	    	CNFEx.printStackTrace();
	    } catch (InterruptedException intEx) {
	    	System.out.println("Error: The client for sensor ID: "+getSensor_ID()+" returns the InterruptedException when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted.");
	    	intEx.printStackTrace();
	    }
    }
	
	public TCPclient initClient(int sensor_ID, String serverHostName, int port) throws IOException{

		return (new TCPclient (sensor_ID, serverHostName, port));
	}
	
	public void closeClient(TCPclient INSTANCE, int port) throws IOException{
		
		if(INSTANCE.getClientSocket() != null){

			INSTANCE.getClientSocket().close();
			System.out.println("Socket for the client with port: "+port+" closed successfully");
			
			// reinitialize clientRunning to false
			clientRunning(false);
		} 
		else {
			throw new IllegalArgumentException();
		}
	}
	
	public void closeClientManager(TCPclient INSTANCE, int port) throws IOException{
		
		if(INSTANCE.getClientManager() != null){
				
			INSTANCE.getClientManager().closeOutStream();
			INSTANCE.getClientManager().closeInStream();
			INSTANCE.getClientManager().setClientManagerRunning(false);
			
			System.out.println("ClientManager for the client with port: "+port+" closed successfully");
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
					break;
				} 
				else {
					itemIndex++; 
				}
			}
			if(itemIndex == (Client_Sensors_LIST.size())) {
				Client_Sensors_LIST.add(sensor);
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
	
	synchronized boolean isClientRunning() {
		return this.clientRunning;
	}
	
	synchronized void clientRunning(boolean isClientRunning) {
	    this.clientRunning = isClientRunning;
	}
	
	public synchronized int getSensor_ID() {
		return sensor_ID;
	}

	public synchronized void setSensor_ID(int sensor_ID) {
		this.sensor_ID = sensor_ID;
	}

}
