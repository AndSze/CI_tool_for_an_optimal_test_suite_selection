package deliverables;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import tcpServer.TCPserver;

public class UUT_TCPserver{
	
    //create the port number
    private int port = 0;
    private TCPserver TCPserver_INSTANCE = null;
    
    UUT_TCPserver(int port) throws IOException {
    	super();
    	this.port = port;
    	this.TCPserver_INSTANCE = new TCPserver();
    }
	
	public static void main(String []args) throws InterruptedException{
		
		int temp_port = 9876;
		
		UUT_TCPserver uut1_TCPserver = null;
		
		try {
			uut1_TCPserver = new UUT_TCPserver(temp_port);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP server at port: "+temp_port+" cannot be created");
			IOEx.printStackTrace();
		}
		
		uut1_TCPserver.setINSTANCE(runTheServer(uut1_TCPserver.getINSTANCE(), uut1_TCPserver.getPort()));
		
		// close the server once TCPserver leaves the infinite while loop - it can happen in case of watchdog expiration
		//uut1_TCPserver.setINSTANCE(closeTheServer(uut1_TCPserver.getINSTANCE(), uut1_TCPserver.getPort()));

	}

	public static TCPserver runTheServer(TCPserver TCPserver_INSTANCE, int port){
		try {
			
			TCPserver_INSTANCE = TCPserver_INSTANCE.initServer(port);
		} catch (BindException BindEx) { // exception happens if program was started more than once
			System.out.println("Error: The server with port: "+port+" already exists and cannot be bound to the requested port ");
			BindEx.printStackTrace();
		} catch (SocketException socketEx) { // exception happens if there was an attempt to run more than server regardless of the port number
	    	System.out.println("Error: The server with port="+port+" returns the SocketException if there is an issue in the underlying protocol, such as a TCP error");
	    	socketEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The server with port="+port+" returns the IOException if the bind operation fails, or if the socket is already bound.");
	    	IOEx.printStackTrace();
	    } 
		return TCPserver_INSTANCE;
		
	}
	
	public static TCPserver closeTheServer(TCPserver TCPserver_INSTANCE, int port)
	{	
		try {
			
			TCPserver_INSTANCE.closeServer(TCPserver_INSTANCE, port);
		
		}  catch (IllegalArgumentException illPTREx ){
			System.out.println("Error: The server with port= "+port+" returns the IllegalArgumentException if there was an attempt to close a server socket that has not been initialized");
			illPTREx.printStackTrace();
		}  catch (SocketException socketEx ){
			System.out.println("Error: The server with port= "+port+" returns the SocketException if there was an attempt to close a socket that runs a thread currently blocked in accept()");
			socketEx.printStackTrace();
		} catch (IOException IOEx ){
			System.out.println("Error: The server with port="+port+" cannot be closed");
			IOEx.printStackTrace();
		} 
		return TCPserver_INSTANCE;
	}
	
	public int getPort() {
		return this.port;
	}

	public TCPserver getINSTANCE() {
		return this.TCPserver_INSTANCE;
	}
	
	public void setINSTANCE(TCPserver TCPserver_INSTANCE) {
		this.TCPserver_INSTANCE = TCPserver_INSTANCE;
	}
}
    
