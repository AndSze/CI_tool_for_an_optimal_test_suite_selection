package deliverables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import messages.SensorState;
import tcpClient.TCPclient;
import watchdog._1h_Watchdog;

public class UUT_TCPclient extends TCPclient{

    //create the port number
    private int port;
    private int sensor_ID;
	final String serverHostName = "localhost";
    private TCPclient INSTANCE = null;
    
    UUT_TCPclient(int sensor_ID, int port) throws IOException {
    	super();
    	this.port = port;
    	this.sensor_ID = sensor_ID;
    	this.INSTANCE = new TCPclient();
    	this.INSTANCE.setSensor_ID(sensor_ID);
    }
    
    public static void main(String []args) throws IOException, InterruptedException{
		
		int temp_port = 9876;
		int temp_sensor_ID = 1;
		
		UUT_TCPclient uut1_TCPclient = null;
		
		try {
			uut1_TCPclient = new UUT_TCPclient(temp_sensor_ID, temp_port);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP client at port: "+temp_port+" cannot be created");
			IOEx.printStackTrace();
		}
		
		
		SensorState current_sensor_state = SensorState.DEAD;
		SensorState previous_sensor_state = SensorState.DEAD;
		
		// client gets the configuration from the server and goes to the operational state
		uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
		
		while(true) {
			if(uut1_TCPclient.getINSTANCE().getClientThread().isAlive() != true ) {
				// close the client socket and the client manager, it will be opened again once the watchdog reaches its threshold
				uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE()));
				uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE()));
				break;
			}
			else {
				Thread.sleep(100);
			}
			
		}
		
		
		
		while (true) {
			current_sensor_state = searchInClientSensorList(uut1_TCPclient.getSensor_ID()).getSensorState();
			if ((_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() < 122) && (current_sensor_state == SensorState.OPERATIONAL) && (uut1_TCPclient.getINSTANCE().isClientRunning() == false)) {
				
				// opens the client socket activates the client manager (out/in object streams)
				uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
			}
			//current_sensor_state = searchInClientSensorList(temp_sensor_ID).getSensorState();
			
			if ((uut1_TCPclient.getINSTANCE().getClientManager().isClientManagerRunning() == false) && (previous_sensor_state == SensorState.OPERATIONAL) && (uut1_TCPclient.getINSTANCE().isClientRunning() == true)){
				// sensors gets go to pre_operational message once it received the ack server message what means that the watchdog has been kicked
				// hence close the client socket and the client manager, it will be opened again once the watchdog reaches its threshold
				uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE()));
				uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE()));                                                                                                                                                                            
			}
			
			if ((_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 1000)) {
				System.out.println("Sensor ID: " + uut1_TCPclient.getSensor_ID() +"\t [TCPClient Main] _1h_Watchdog: "+ _1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
				Thread.sleep(5000);
				
			}
			else {
				Thread.sleep(10);
			}
			//previous_sensor_state = searchInClientSensorList(temp_sensor_ID).getSensorState();
			
		}
		
		
     }

	public static TCPclient runTheClient(TCPclient INSTANCE, int port, String serverHostName){
		try {
			INSTANCE = INSTANCE.initClient(INSTANCE.getSensor_ID(), serverHostName, port);
			
			Thread temp = new Thread(INSTANCE);
			temp.start();
			INSTANCE.setClientThread(temp);
			
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
