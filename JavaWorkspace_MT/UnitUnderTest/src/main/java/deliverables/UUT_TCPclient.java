package deliverables;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import tcpClient.TCPclient;

public class UUT_TCPclient {

    //create the port number
    static int port = 9877;
    static String serverHostName = "localhost";
	
	public static void main(String []args) throws IOException, InterruptedException{
		
		TCPclient INSTANCE =  new TCPclient();
		
		runTheClient(INSTANCE,port, serverHostName);
		
		for (int i=0;i<5;i++){
			
			 System.out.println("Sending message "+i);
	         String message = Integer.toString(i)+"\n";
	            
            //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	        INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(), message);
	        //Thread.sleep(100);
        }
		
		closeTheClient(INSTANCE, port);
        System.out.println("Mission Completed");
        
        runTheClient(INSTANCE,port, serverHostName);
		
		for (int i=0;i<5;i++){
			
			 System.out.println("2 Sending message "+i*i);
	         String message = Integer.toString(i*i)+"\n";
	            
            //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	        INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(), message);
	        //Thread.sleep(100);
        }
		
		closeTheClient(INSTANCE, port);
        System.out.println("Mission Completed");
        
        runTheClient(INSTANCE,port, serverHostName);
		
		for (int i=0;i<5;i++){
			
			 System.out.println("3 Sending message "+i*i*i);
	         String message = Integer.toString(i*i*i)+"\n";
	            
            //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	        INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(), message);
	        //Thread.sleep(100);
        }
		
		System.out.println("Mission Completed");
		
		closeTheClient(INSTANCE, port);
        
     }

	public static void runTheClient(TCPclient INSTANCE, int port, String serverHostName){
		try {
			INSTANCE.initClient(serverHostName, port);
			
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
	}
	
	public static void closeTheClient(TCPclient INSTANCE, int port){	
		try {
			INSTANCE.closeClient(INSTANCE.getClientSocket(), port);
		} catch (IOException IOEx ){
			System.out.println("Error: The client socket with port="+port+" cannot be closed on the server side");
			IOEx.printStackTrace();
		}
	}	
}
