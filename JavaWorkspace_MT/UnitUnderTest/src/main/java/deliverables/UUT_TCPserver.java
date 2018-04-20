package deliverables;

import java.io.IOException;
import java.rmi.RemoteException;
import udpServer.TCPserver;

public class UUT_TCPserver {
	
    //create the port number
    static int port = 9876;
	
	public static void main(String []args) throws IOException{
	    
		TCPserver.getInstance().initServer(port);
	}

}
    
