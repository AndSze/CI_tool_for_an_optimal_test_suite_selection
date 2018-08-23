package deliverables;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import tcpServer.TCPserver;

public class UUT_TCPserver{
	
    private static int port = 0;
	private static int number_of_sensors = 0;
    private static double watchdog_scale_factor = 0.0;
    private static int measurements_limit = 0;
	
    /***********************************************************************************************************
	 * Method Name: 				UUT_TCPclient()
	 * Description: 				UUT_TCPclient class default constructor
	 * Affected internal variables: port, inputStream, sensor_ID, INSTANCE, serverHostName, delays_array, watchdog_thresholds_array
	 * Affected external variables: TCPclient.sensor_ID
	 * Exceptions thrown: 			IOException
     * @throws IOException 
	 ***********************************************************************************************************/
	UUT_TCPserver(int port, int number_of_sensors, int measurements_limit, double watchdog_scale_factor) throws IOException{
		
		super();
		
		setPort(port);
		setNumber_of_sensors(number_of_sensors);
		setWatchdog_scale_factor(watchdog_scale_factor);
		setMeasurements_limit(measurements_limit);
		
		// call the default constructor of the TCPserver class
		TCPserver.getInstance(getPort(), getNumber_of_sensors(), getMeasurements_limit(), getWatchdog_scale_factor());

    }

    /***********************************************************************************************************
	* Method Name: 					public static void main() 
	* Description: 				   	Calls constructors of the TCPserver class 
	* Called external functions:   	TCPserver.getInstance()
	* Exceptions handled: 		   	IOException, BindException, SocketException
	***********************************************************************************************************/
	public static void main(int port, int number_of_sensors, int measurements_limit, double watchdog_scale_factor){
		
		// local variable that determines the port on which the TCP communication is going to take place
		@SuppressWarnings("unused")
		UUT_TCPserver uut_tcp_server = null;
		
		// call the default constructor of the TCPserver class
		try {
			uut_tcp_server = new UUT_TCPserver(port, number_of_sensors, measurements_limit, watchdog_scale_factor);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP server at port: "+port+" cannot be created");
			IOEx.printStackTrace();
		}
		
		// call the overloaded constructor of the TCPserver class that triggers entire communication handling on the server side
		try {	
			TCPserver.getInstance(getPort(), getNumber_of_sensors(), getMeasurements_limit(), getWatchdog_scale_factor());
		} catch (BindException BindEx) { // exception is being thrown if program was started more than once
			System.out.println("Error: The server with port: "+getPort()+" already exists and cannot be bound to the requested port ");
			BindEx.printStackTrace();
		} catch (SocketException socketEx) { // exception is being thrown if there was an attempt to run more than one server instance regardless of the port number
	    	System.out.println("Error: The server with port="+getPort()+" returns the SocketException if there is an issue in the underlying protocol, such as a TCP error");
	    	socketEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The server with port="+getPort()+" returns the IOException if the bind operation fails, or if the socket is already bound.");
	    	IOEx.printStackTrace();
	    } 
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public static TCPclient closeTheClient()
	 * Description: 				closes TCP connection with server by calling closeClient() function for TCPclient INSTANCE
	 * Affected external variables: TCPclient.clientThread
	 * Returned value				INSTANCE
	 * Called external functions:   TCPclient.closeClient()
	 * Exceptions handled: 			IllegalArgumentException, IOException
	 ***********************************************************************************************************/
	public void closeTheServer(){
		

		try {
			
			TCPserver.getInstance(getPort(), getNumber_of_sensors(), getMeasurements_limit(), getWatchdog_scale_factor()).closeServer(getPort());
			
		} catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: UUT TCP server returns IllegalArgumentException if there was an attempt to close the server socket that has not been initialized");
			illPTREx.printStackTrace();
		}catch (IOException IOEx ){
			System.out.println("Error: UUT TCP server returns IOException if  the server socket cannot be closed");
			IOEx.printStackTrace();
		}
	}	
	
    public static int getPort() {
		return port;
	}

	public void setPort(int port) {
		UUT_TCPserver.port = port;
	}

	public static int getNumber_of_sensors() {
		return number_of_sensors;
	}

	public void setNumber_of_sensors(int number_of_sensors) {
		UUT_TCPserver.number_of_sensors = number_of_sensors;
	}

	public static double getWatchdog_scale_factor() {
		return watchdog_scale_factor;
	}

	public void setWatchdog_scale_factor(double watchdog_scale_factor) {
		UUT_TCPserver.watchdog_scale_factor = watchdog_scale_factor;
	}

	public static int getMeasurements_limit() {
		return measurements_limit;
	}

	public void setMeasurements_limit(int measurements_limit) {
		UUT_TCPserver.measurements_limit = measurements_limit;
	}
}
    
