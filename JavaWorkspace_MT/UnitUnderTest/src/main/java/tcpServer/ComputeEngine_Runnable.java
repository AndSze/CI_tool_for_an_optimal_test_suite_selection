package tcpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ComputeEngine_Runnable extends TCPserver implements Runnable {
	
    private static int computeEnginesRunningID   = 0;
    // ComputeEngine_Runnable_ID is set to ID of the sensor for which the communication is being executed in this ComputeEngine instance
    private int ComputeEngine_Runnable_ID = 0;
    private PrintStream outputStream = null;
    private InputStreamReader inputStream = null;
    private int timeout = 0;
	
	public ComputeEngine_Runnable(Socket clientSocket) throws IOException  {
		super();
		inputStream = new InputStreamReader(clientSocket.getInputStream());
    	outputStream = new PrintStream(clientSocket.getOutputStream(), true);
    	ComputeEngine_Runnable.computeEnginesRunningID  += 1;
        System.out.println("[ECHO Compute engine] Multithreaded Server Service for processing Client Request no: "+ ComputeEngine_Runnable.computeEnginesRunningID + " has been started");

	}

    public void run() {
      
    	//synchronized (Echo) {
    		
    		try {
    		BufferedReader bufferedReader = new BufferedReader(inputStream);
            String message = null;
            timeout = 0;
            
            while(timeout<10)
            {
    			if(bufferedReader.ready())
    			{
	            	long time = System.currentTimeMillis();
	            	message = bufferedReader.readLine();
	            	EchoResponse(outputStream, message, time);
	            	timeout = 0;
    			}
    			else
    			{
    				timeout = timeout+1;
    				processingDelay(1000);
    			}
    			//processingDelay(10);
            }  
        } catch (IOException IOex) {
        	System.out.println("Error: when attempted to read bufferedReaderinputStream on the client side");
        	IOex.printStackTrace();
        } finally {
        		closeOutStream();
        	try {
				closeInStream();
			} catch (IOException IOex) {
			    System.out.println("Error: when attempted to close InputStreamReader inputStream on the client side");
			    IOex.printStackTrace();
			}
          }
    	//}
    }
    

	public /*synchronized*/ void EchoResponse(PrintStream outputStream, String message, long time) {

		String server_message = null;
		
        System.out.println("message received from cliennnt: \n\t"+message);
        //processingDelay(1000);
        server_message =  Integer.toString(computeEnginesRunningID*Integer.parseInt(message));
        server_message = "Let's try ComputerEngine ID: " + computeEnginesRunningID+ " that resends: "+server_message;
        
        System.out.println("Send back the following message: "+server_message);
        
        outputStream.println(server_message);
        System.out.println("Request processed: " + time);
	}
	
	public void closeOutStream() {
		if (outputStream!=null) {
			outputStream.close();
		}
	}
	
	public void closeInStream() throws IOException {
		if (inputStream!=null) {
			inputStream.close();
		}
	}
	
	static void processingDelay(int msec) {
	    try {
	        Thread.sleep(msec);
	    } catch (InterruptedException ex) {
	        
	    }
    }
}

