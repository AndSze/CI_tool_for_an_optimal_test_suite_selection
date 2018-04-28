package deliverables;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import tcpServer.TCPserver;

public class UUT_TCPserver {
	
    //create the port number
    private int port = 0;
    //Arraylist<TCPserver> servers = null;
    private TCPserver INSTANCE = null;
    
    UUT_TCPserver(int port) throws IOException {
    	super();
    	this.port = port;
    	this.INSTANCE = new TCPserver();
    }
	
	public static void main(String []args) throws InterruptedException{
		
		UUT_TCPserver uut1_TCPserver = null;
		UUT_TCPserver uut2_TCPserver = null;
		try {
			uut1_TCPserver = new UUT_TCPserver(9877);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP server cannot be created");
			IOEx.printStackTrace();
		}
		
		uut1_TCPserver.setINSTANCE(runTheServer(uut1_TCPserver.getINSTANCE(), uut1_TCPserver.getPort()));
		
		Thread.sleep(100);

		uut1_TCPserver.setINSTANCE(closeTheServer(uut1_TCPserver.getINSTANCE(), 9877));
		
		Thread.sleep(100);
		
		try {
			uut2_TCPserver = new UUT_TCPserver(9877);
		} catch (IOException IOEx) {
			System.out.println("Error: Instance for the TCP server cannot be created");
			IOEx.printStackTrace();
		}
		
		uut2_TCPserver.setINSTANCE(runTheServer(uut2_TCPserver.getINSTANCE(), uut2_TCPserver.getPort()));
		
		Thread.sleep(100);


	}

	public static TCPserver runTheServer(TCPserver INSTANCE, int port){
		try {
			
			INSTANCE = INSTANCE.initServer(port);
			
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
		return INSTANCE;
		
	}
	
	public static TCPserver closeTheServer(TCPserver INSTANCE, int port)
	{	
		try {
			
			INSTANCE.closeServer(INSTANCE, port);
		
		}  catch (IllegalArgumentException nullPTREx ){
			System.out.println("Error: The server with port= "+port+" returns the IllegalArgumentException if there was an attempt to close a socket that has not been initialized");
			nullPTREx.printStackTrace();
		}  catch (SocketException socketEx ){
			System.out.println("Error: The server with port= "+port+" returns the SocketException if there was an attempt to close a socket that runs a thread currently blocked in accept()");
			socketEx.printStackTrace();
		} catch (IOException IOEx ){
			System.out.println("Error: The server with port="+port+" cannot be closed");
			IOEx.printStackTrace();
		} 
		return INSTANCE;
	}
	
	
	public int getPort() {
		return this.port;
	}

	public TCPserver getINSTANCE() {
		return this.INSTANCE;
	}
	
	public void setINSTANCE(TCPserver INSTANCE) {
		this.INSTANCE = INSTANCE;
	}
}
    
