package tcpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ComputeEngine implements TCPserver_interface, Runnable {
	
    protected String serverText   = null;
    protected PrintStream outputStream = null;
    protected InputStreamReader inputStream = null;
    protected int timeout = 0;
	
	public ComputeEngine(Socket clientSocket, String serverText) throws ClassNotFoundException, IOException  {
		super();
		inputStream = new InputStreamReader(clientSocket.getInputStream());
    	outputStream = new PrintStream(clientSocket.getOutputStream(), true);
		this.serverText   = serverText;
    	System.out.println(Thread.currentThread().getName());

	}

    public void run() {
        try {
        	System.out.println(Thread.currentThread().getName());
        	
       	
            BufferedReader bufferedReader = new BufferedReader(inputStream);
            String message = null;
            timeout = 0;
            
            while(timeout<1010)
            {
    			if(bufferedReader.ready())
    			{
	            	long time = System.currentTimeMillis();
	            	message = bufferedReader.readLine();
	            	Echo(outputStream, message, time);
	            	timeout = 0;
    			}
    			else
    			{
    				timeout = timeout+1;
    				processingDelay(10);
    			}
    			//processingDelay(10);
            }
            
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
        finally {
        	closeOutStream(outputStream);
        	try {
				closeInStream(inputStream);
			} catch (IOException IOex) {
			    System.out.println("Error: when attempted to close InputStreamReader inputStream");
            	System.out.println(IOex.getMessage());
			}
        }
    }
    

	public void processClinetMessage() {
		// TODO Auto-generated method stub
		
	}
	public synchronized void Echo(PrintStream outputStream, String message, long time) {

		String server_message = null;
		
        System.out.println("message received from cliennnt: \n\t"+message);
        //processingDelay(1000);
        server_message = "Let's try "+message;
        
        System.out.println("Send back the following message: "+server_message);
        
        outputStream.println(server_message);
        System.out.println("Request processed: " + time);
	}

	public void sendMessage() {
		// TODO Auto-generated method stub
	}
	
	public void closeOutStream(PrintStream outputStream) {
		if (outputStream!=null) {
			outputStream.close();
		}
	}
	
	public void closeInStream(InputStreamReader inputStream) throws IOException {
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
