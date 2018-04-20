package deliverables;

import java.io.IOException;
import udpClient.TCPclient;

public class UUT_TCPclient {

    //create the port number
    static int port = 9876;
    static String serverHostName = "localhost";
	private static TCPclient INSTANCE;
	
	public static void main(String []args) throws IOException, InterruptedException{
		
		INSTANCE = TCPclient.getInstance();
		INSTANCE.initClient(serverHostName, port);
		
		
		for (int i=0;i<20;i++){
			
			 System.out.println("Sending message "+i);
	         String message = Integer.toString(i)+"\n";
	            
            //if(!INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message)) break;
	        INSTANCE.EchoMessageHandler(INSTANCE.getClientSocket(),message);
	        Thread.sleep(3000);
        }
		INSTANCE.closeClientr(INSTANCE.getClientSocket(), port);
        System.out.println("Mission Completed");
     }
}
