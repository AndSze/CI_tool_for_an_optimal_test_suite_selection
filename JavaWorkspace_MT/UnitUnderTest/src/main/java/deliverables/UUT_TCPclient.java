package deliverables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import tcpClient.TCPclient;

public class UUT_TCPclient {

    //create the port number
    private int port = 0;
    final String serverHostName = "localhost";
    private TCPclient INSTANCE = null;
    
    UUT_TCPclient(int port) throws IOException {
    	super();
    	this.port = port;
    	this.INSTANCE = new TCPclient();
    }
    
	public static void main(String []args) throws IOException, InterruptedException{
		
		int temp_port = 9876;
		UUT_TCPclient uut1_TCPclient = null;
		
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
		
		uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
        System.out.println("Mission Completed");
        
		uut1_TCPclient.setINSTANCE(runTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort(), uut1_TCPclient.getServerHostName()));
		
		for (int i=0;i<5;i++){
			
			 System.out.println("2 Sending message "+i*i);
	         String message = Integer.toString(i*i)+"\n";
	            
	       //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	         uut1_TCPclient.getINSTANCE().EchoMessageHandler(uut1_TCPclient.getINSTANCE().getClientSocket(), message);
	        //Thread.sleep(100);00);
        }
		
		uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
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
		
		uut1_TCPclient.setINSTANCE(closeTheClient(uut1_TCPclient.getINSTANCE(),uut1_TCPclient.getPort()));
        
     }

	public static TCPclient runTheClient(TCPclient INSTANCE, int port, String serverHostName){
		try {
			INSTANCE = INSTANCE.initClient(serverHostName, port);
			
		} catch (ConnectException connectEx) {
	    	System.out.println("Error: The client with port= "+port+" returns the ConnectException while attempting to connect a socket to a remote address and port. Typically, the connection was refused remotely");
	    	connectEx.printStackTrace();
		} catch (SocketException socketEx) {
	    	System.out.println("Error: The client with port="+port+" returns the SocketException if there is an issue in the underlying protocol, such as a TCP error");
	    	socketEx.printStackTrace();
	    } catch (IOException IOEx) {
	    	System.out.println("Error: The client with port="+port+" returns the IOException if the bind operation fails, or if the socket is already bound.");
	    	IOEx.printStackTrace();
	    }
		return INSTANCE;
	}
	
	public static TCPclient closeTheClient(TCPclient INSTANCE, int port){	
		try {
			INSTANCE.closeClient(INSTANCE.getClientSocket(), port);
		} catch (IOException IOEx ){
			System.out.println("Error: The client socket with port="+port+" cannot be closed on the server side");
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
}
