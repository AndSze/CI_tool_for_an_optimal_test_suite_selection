package deliverables;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;

import tcpServer.TCPserver;

public class UUT_TCPserver {
	
    //create the port number
    private int port = 0;
    private TCPserver INSTANCE = null;
    
    UUT_TCPserver(int port) throws IOException {
    	super();
    	this.port = port;
    	this.INSTANCE = TCPserver.getInstance();
    }
	
	public static void main(String []args) throws InterruptedException{
		
		UUT_TCPserver uut_TCPserver1 = null;
		try {
			uut_TCPserver1 = new UUT_TCPserver(9876);
		} catch (IOException IOEx) {
			System.out.println("Error: The server socket cannot be created");
			IOEx.printStackTrace();
		}
		
		runTheServer(uut_TCPserver1.getINSTANCE(), uut_TCPserver1.getPort());
		
		Thread.sleep(100);

		closeTheServer(uut_TCPserver1.getINSTANCE(), 9876);
		
		Thread.sleep(100);
		
		try {
			uut_TCPserver1 = new UUT_TCPserver(9876);
		} catch (IOException IOEx) {
			System.out.println("Error: The server socket cannot be created");
			IOEx.printStackTrace();
		}
		
		runTheServer(uut_TCPserver1.getINSTANCE(), uut_TCPserver1.getPort());

	}

	public static void runTheServer(TCPserver INSTANCE, int port){
		try {
			
			INSTANCE.initServer(port);
			
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
	}
	
	public static void closeTheServer(TCPserver INSTANCE, int port)
	{	
		try {
			
			INSTANCE.closeServer(port);
			
		} catch (RuntimeException RTEx ){
			System.out.println("Catched Runtime Exception that is caused by the fact that the server socket has been closed ");
			RTEx.printStackTrace();
		} catch (IOException IOEx ){
			System.out.println("Error: The server with port="+port+" cannot be closed");
			IOEx.printStackTrace();
		}
	}
	
	
	public int getPort() {
		return this.port;
	}

	public TCPserver getINSTANCE() {
		return this.INSTANCE;
	}
}
    
