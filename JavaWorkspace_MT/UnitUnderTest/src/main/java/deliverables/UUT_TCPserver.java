package deliverables;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import tcpServer.TCPserver;

public class UUT_TCPserver{
	
    /***********************************************************************************************************
	 * Method Name: public static void main(String []args)
	 * Description: Calls constructors of the TCPserver class 
	 * Affected external variables: TCPserver_instance
	 * Called external functions: TCPserver.getInstance()
	 * Exceptions handled: IOException, BindException, SocketException
	 ***********************************************************************************************************/
	public static void main(String []args){
		
		// local variable that determines the port on which the TCP communication is going to take place
		int temp_port = 9876;
		
		// call the default constructor of the TCPserver class
		try {
			TCPserver.getInstance(temp_port);
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
}
    
