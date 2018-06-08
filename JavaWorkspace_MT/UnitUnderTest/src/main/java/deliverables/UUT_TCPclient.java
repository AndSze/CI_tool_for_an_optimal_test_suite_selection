package deliverables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import messages.SensorState;
import sensor.SensorImpl;
import tcpClient.TCPclient;
import watchdog.Local_1h_Watchdog;

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
		int temp_sensor_ID = 3;
		
		UUT_TCPclient uut1_TCPclient = null;
		
		try {
			uut1_TCPclient = new UUT_TCPclient(temp_sensor_ID, temp_port);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP client at port: "+temp_port+" cannot be created");
			IOEx.printStackTrace();
		}
		
		
		SensorState current_sensor_state = SensorState.DEAD;
		SensorState previous_sensor_state = SensorState.DEAD;		
		
		//System.out.println("Client_Sensors_LIST size: "+ Client_Sensors_LIST.size());
		
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
		
		SensorImpl UUT_sensor_instance = null;
		
		int print_loop_count = 0;
		while (true) {
			UUT_sensor_instance = searchInClientSensorList(uut1_TCPclient.getSensor_ID());
			current_sensor_state = UUT_sensor_instance.getSensorState();
			uut1_TCPclient.getINSTANCE();
			if ((Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() < 120 * UUT_sensor_instance.getLocal_watchdog_scale_factor()) && (current_sensor_state == SensorState.OPERATIONAL) && (uut1_TCPclient.getINSTANCE().isClientRunning() == false)) {
																			
				// opens the client socket activates the client manager (out/in object streams)
				System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t runTheClient() is being called");
				uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
			}
			//current_sensor_state = searchInClientSensorList(temp_sensor_ID).getSensorState();
			
			if ((uut1_TCPclient.getINSTANCE().getClientManager().isClientManagerRunning() == false) && (previous_sensor_state == SensorState.OPERATIONAL) && (uut1_TCPclient.getINSTANCE().isClientRunning() == true)){
				// sensors gets go to pre_operational message once it received the ack server message what means that the watchdog has been kicked
				// hence close the client socket and the client manager, it will be opened again once the watchdog reaches its threshold
				System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t closeTheClientManager() & closeTheClient() are being called");
				
				// closeTheClientManager closes input/output object stremas for the ClientManager that has been already closed
				uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE()));
				uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE()));  
			}
			
			if (Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > (200 * UUT_sensor_instance.getLocal_watchdog_scale_factor()) ) {
				print_loop_count++;
				if (print_loop_count == 10) {
					System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t  Local_1h_Watchdog: "+ Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
					print_loop_count = 0;
				}
				if (UUT_sensor_instance.getLocal_watchdog_scale_factor() >= 1.0) {
					System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t  I've got stucked here");
					Thread.sleep(50000);
				}
				else if (UUT_sensor_instance.getLocal_watchdog_scale_factor() >= 0.1){
					Thread.sleep(5000);
				}
				else {
					Thread.sleep(500);
				}
				
			}
			else if (Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > (100 * UUT_sensor_instance.getLocal_watchdog_scale_factor())) {
				System.out.println("[UUT_TCPclient " + uut1_TCPclient.getSensor_ID() + "]\t  Local_1h_Watchdog: "+ Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
				if (UUT_sensor_instance.getLocal_watchdog_scale_factor() >= 1.0) {
					Thread.sleep(10000);
				}
				else if (UUT_sensor_instance.getLocal_watchdog_scale_factor() >= 0.1){
					Thread.sleep(1000);
				}
				else {
					Thread.sleep(100);
				}
			}
			else {
				Thread.sleep(10);
			}
			previous_sensor_state = searchInClientSensorList(temp_sensor_ID).getSensorState();
			
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
