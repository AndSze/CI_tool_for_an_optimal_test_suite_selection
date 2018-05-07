package tcpServer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.rmi.RemoteException;

public class ComputeEngine implements TCPserver_interface, Runnable {
	
    private static int computeEnginesRunningID   = 0;
    private PrintStream outputStream = null;
    private InputStreamReader inputStream = null;
    private int timeout = 0;
    final Object Echo = new Object();
	
	public ComputeEngine(Socket clientSocket) throws IOException  {
		super();
		inputStream = new InputStreamReader(clientSocket.getInputStream());
    	outputStream = new PrintStream(clientSocket.getOutputStream(), true);
    	ComputeEngine.computeEnginesRunningID  += 1;
        System.out.println("[ECHO Compute engine] Multithreaded Server Service for processing Client Request no: "+ ComputeEngine.computeEnginesRunningID + " has been started");

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
	
	public boolean serialize(Object obj, String path) throws RemoteException, ClassNotFoundException
	{
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in " + path);
			return true;
		} catch (IOException i) {
			i.printStackTrace();
			return false;
		}
	}

	public Object deserialize(String path) throws RemoteException, ClassNotFoundException
	{
		Object obj = null;
		try {
			FileInputStream fileIn = new FileInputStream(path);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			obj = in.readObject();
			in.close();
			fileIn.close();
			//System.out.println("Serialized data is retrieved from " + path);
			return obj;
		} catch (IOException i) {
			i.printStackTrace();
			return obj;
		}
	}

	public void processClinetMessage() {
		// TODO Auto-generated method stub
		
	}

	public void sendMessage() {
		// TODO Auto-generated method stub
		
	}

}
