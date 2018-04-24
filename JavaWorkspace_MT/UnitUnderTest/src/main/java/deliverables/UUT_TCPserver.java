package deliverables;

import java.io.IOException;
import java.net.BindException;

import tcpServer.TCPserver;

public class UUT_TCPserver {
	
    //create the port number
    private int port = 0;
    
    private UUT_TCPserver(int port) {
    	super();
    	this.port = port;
    }
	
	public static void main(String []args) throws IOException{
		
		UUT_TCPserver uut_TCPserver = new UUT_TCPserver(9876);
		TCPserver.getInstance().initServer(uut_TCPserver.getPort());
	}

	public int getPort() {
		return this.port;
	}

}
    
