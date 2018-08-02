package deliverables;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import tcpServer.TCPserver;

public class UUT_TCPserver{
	
    /***********************************************************************************************************
	 * Method Name: 				UUT_TCPclient()
	 * Description: 				UUT_TCPclient class default constructor
	 * Affected internal variables: port, inputStream, sensor_ID, INSTANCE, serverHostName, delays_array, watchdog_thresholds_array
	 * Affected external variables: TCPclient.sensor_ID
	 * Exceptions thrown: 			IOException
     * @throws IOException 
	 ***********************************************************************************************************/
	UUT_TCPserver(int port) throws IOException{
		// call the default constructor of the TCPserver class
		TCPserver.getInstance(port);
    }

    /***********************************************************************************************************
	* Method Name: 					public static void main(String []args) 
	* Description: 				   	Calls constructors of the TCPserver class 
	* Called external functions:   	TCPserver.getInstance()
	* Exceptions handled: 		   	IOException, BindException, SocketException
	***********************************************************************************************************/
	public static void main(String []args){
		
		// local variable that determines the port on which the TCP communication is going to take place
		int temp_port = 9876;
		@SuppressWarnings("unused")
		UUT_TCPserver uut_tcp_server = null;
		
		// call the default constructor of the TCPserver class
		try {
			uut_tcp_server = new UUT_TCPserver(temp_port);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP server at port: "+temp_port+" cannot be created");
			IOEx.printStackTrace();
		}
		
		// call the overloaded constructor of the TCPserver class that triggers entire communication handling on the server side
		try {	
			TCPserver.getInstance(temp_port);
		} catch (BindException BindEx) { // exception is being thrown if program was started more than once
			System.out.println("Error: The server with port: "+temp_port+" already exists and cannot be bound to the requested port ");
			BindEx.printStackTrace();
		} catch (SocketException socketEx) { // exception is being thrown if there was an attempt to run more than one server instance regardless of the port number
	    	System.out.println("Error: The server with port="+temp_port+" returns the SocketException if there is an issue in the underlying protocol, such as a TCP error");
	    	socketEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The server with port="+temp_port+" returns the IOException if the bind operation fails, or if the socket is already bound.");
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
		
		int temp_port = 9876;
		
		try {
			
			TCPserver.getInstance(temp_port).closeServer(TCPserver.getInstance(temp_port), temp_port);
			
		} catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: UUT TCP server returns IllegalArgumentException if there was an attempt to close the server socket that has not been initialized");
			illPTREx.printStackTrace();
		}catch (IOException IOEx ){
			System.out.println("Error: UUT TCP server returns IOException if  the server socket cannot be closed");
			IOEx.printStackTrace();
		}
	}	
}
    
