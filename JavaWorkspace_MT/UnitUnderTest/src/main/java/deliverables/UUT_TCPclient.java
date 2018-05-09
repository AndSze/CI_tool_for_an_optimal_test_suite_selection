package deliverables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import sensor.SensorImpl;
import tcpClient.TCPclient;
import watchdog.ClientWatchdog;


public class UUT_TCPclient extends TCPclient{

    //create the port number
    private int port = 0;
    final String serverHostName = "localhost";
    private TCPclient INSTANCE = null;
    private static ClientWatchdog clientWatchdog_INSTANCE = null;
    
    UUT_TCPclient(int port) throws IOException {
    	super();
    	this.port = port;
    	this.INSTANCE = new TCPclient();
    	UUT_TCPclient.clientWatchdog_INSTANCE = ClientWatchdog.getInstance();
    }
    
	public static void main(String []args) throws IOException, InterruptedException{
		
		int temp_port = 9876;
		int no_of_sensors = 8;
		UUT_TCPclient uut1_TCPclient = null;
		
		// create instances of sensors on the client side and add them to the Client_Sensors_LIST
		for (int i = 1; i <= no_of_sensors; i++) {
			Client_Sensors_LIST = updateClientSensorList(new SensorImpl(i));
		}
		
		try {
			uut1_TCPclient = new UUT_TCPclient(temp_port);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP client at port: "+temp_port+" cannot be created");
			IOEx.printStackTrace();
		}
		
		uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
		
		for (int i=0;i<5;i++){
			
			 System.out.println("Sending message "+i);
	         String message = Integer.toString(i)+"\n";
	            
            //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	         uut1_TCPclient.getINSTANCE().EchoMessageHandler(uut1_TCPclient.getINSTANCE().getClientSocket(), message);
	        //Thread.sleep(100);
        }
		
		System.out.println("ClientWatchdog TimeFromLastFeed: " + clientWatchdog_INSTANCE.getTimeFromLastFeed());
		System.out.println("ClientWatchdog TimeLeftBeforeExpiration: " + clientWatchdog_INSTANCE.getTimeLeftBeforeExpiration());
		clientWatchdog_INSTANCE.feed();
		
		uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
		uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
        System.out.println("Mission Completed");
        
		uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
		
		for (int i=0;i<5;i++){
			
			 System.out.println("2 Sending message "+i*i);
	         String message = Integer.toString(i*i)+"\n";
	            
	       //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	         uut1_TCPclient.getINSTANCE().EchoMessageHandler(uut1_TCPclient.getINSTANCE().getClientSocket(), message);
	        //Thread.sleep(100);00);
        }
		
		System.out.println("ClientWatchdog TimeFromLastFeed: " + clientWatchdog_INSTANCE.getTimeFromLastFeed());
		System.out.println("ClientWatchdog TimeLeftBeforeExpiration: " + clientWatchdog_INSTANCE.getTimeLeftBeforeExpiration());
		clientWatchdog_INSTANCE.feed();
		
		uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
		uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
        System.out.println("Mission Completed");
        
		uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
		
		for (int i=0;i<5;i++){
			
			 System.out.println("3 Sending message "+i*i*i);
	         String message = Integer.toString(i*i*i)+"\n";
	            
	       //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	         uut1_TCPclient.getINSTANCE().EchoMessageHandler(uut1_TCPclient.getINSTANCE().getClientSocket(), message);
	        //Thread.sleep(100);;
        }
		
		System.out.println("Mission Completed");
		
		System.out.println("ClientWatchdog TimeFromLastFeed: " + clientWatchdog_INSTANCE.getTimeFromLastFeed());
		System.out.println("ClientWatchdog TimeLeftBeforeExpiration: " + clientWatchdog_INSTANCE.getTimeLeftBeforeExpiration());
		
		uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
		uut1_TCPclient.setINSTANCE(closeTheClientManager(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));

        
     }

	public static TCPclient runTheClient(TCPclient INSTANCE, int port, String serverHostName){
		try {
			INSTANCE = INSTANCE.initClient(serverHostName, port, clientWatchdog_INSTANCE);
			
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
	
	public static TCPclient closeTheClient(TCPclient INSTANCE, int port){	
		try {
			INSTANCE.closeClient(INSTANCE, port);
		} catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: The client with port= "+port+" returns the IllegalArgumentException if there was an attempt to close a client socket that has not been initialized");
			illPTREx.printStackTrace();
		}catch (IOException IOEx ){
			System.out.println("Error: The client socket with port="+port+" cannot be closed on the server side");
			IOEx.printStackTrace();
		}
		return INSTANCE;
	}	
	
	public static TCPclient closeTheClientManager(TCPclient INSTANCE, int port){	
		try {
			INSTANCE.closeClientManager(INSTANCE, port);
		} catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: The client with port= "+port+" returns the IllegalArgumentException if there was an attempt to close a client manager that has not been initialized");
			illPTREx.printStackTrace();
		}catch (IOException IOEx ){
			System.out.println("Error: The client socket with port="+port+" cannot be closed on the server side");
			IOEx.printStackTrace();
		}
		return INSTANCE;
	}	
	
	public static ArrayList<SensorImpl> updateClientSensorList(SensorImpl sensor){
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
}
