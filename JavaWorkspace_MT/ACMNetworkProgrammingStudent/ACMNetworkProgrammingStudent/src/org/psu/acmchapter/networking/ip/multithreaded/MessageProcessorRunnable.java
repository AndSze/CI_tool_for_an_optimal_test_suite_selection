package org.psu.acmchapter.networking.ip.multithreaded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class MessageProcessorRunnable implements Runnable{

    protected Socket clientSocket = null;
    protected String serverText   = null;
    protected PrintStream outputStream;
    protected InputStreamReader inputStream;
    
    public MessageProcessorRunnable(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
    }

    public void run() {
        try {
        	
        	PrintStream outputStream = new PrintStream(clientSocket.getOutputStream(), true);
            InputStreamReader inputStream = new InputStreamReader(clientSocket.getInputStream());
            
            BufferedReader stdIn =
                    new BufferedReader(new InputStreamReader(System.in));
  
            long time = System.currentTimeMillis();
            
            BufferedReader bufferedReader = new BufferedReader(inputStream);
            String message, server_message = null;
            message = bufferedReader.readLine();
            
            System.out.println("message received from cliennnt: \n\t"+message);
            processingDelay(1000);
            server_message = "Let's try "+message;
            
            while ((server_message = stdIn.readLine()) != null) {
                System.out.println("Send back the following message: "+server_message);  
                outputStream.print(server_message);
            }  
            
     
            System.out.println("Request processed: " + time);
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
        finally {
        	if (outputStream!=null) {
        		outputStream.close();
        	}
        	if (inputStream!=null) {
        		try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
    }
    public static void processingDelay(int msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException ex) {
            
        }
    }
}

