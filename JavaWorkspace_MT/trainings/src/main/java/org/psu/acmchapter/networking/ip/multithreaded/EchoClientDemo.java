package org.psu.acmchapter.networking.ip.multithreaded;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;


public class EchoClientDemo {
    
    public static void main(String args[]) throws IOException, InterruptedException { 
        System.out.print("/* * * * * * * * * * * * * * * * * * * * * * * * * * */\n"
                + "\t\tE C H O   C L I E N T\n"
                + "/* * * * * * * * * * * * * * * * * * * * * * * * * * */\n");
        
         Socket clientSocket =null;
         String serverHostName = "localhost";
         
         int port = 3030;
        
        Scanner s = new Scanner(System.in);
        //System.out.println("Press a key to send the message ...");
        //s.next();
        //prepare the String. WARNING: add \n to make sure that the message is considered as one line

        
        boolean success;
        
        for (int i=0;i<20;i++){
        	
        	success = sendTo(clientSocket, serverHostName, port, i);
            if(!success) break;
        }
        System.out.println("Mission Completed");
     }
    
    public static boolean sendTo(Socket clientSocket, String serverHostName, int port, int i) {
    	
    	boolean sent = false;
    	 
    	try {
            clientSocket = new Socket(serverHostName, port);
        
            PrintStream printStream = new PrintStream(clientSocket.getOutputStream());
            InputStreamReader inputStream = new InputStreamReader(clientSocket.getInputStream());
            System.out.println("Sending message "+i);
            String message = Integer.toString(i)+"\n";
            long t0 = System.currentTimeMillis();
  
            // it activates serverSocket.accept() on the server side
	        printStream.print(message);
            //Thread.sleep(1000);
            BufferedReader bufferedReader = new BufferedReader(inputStream);
            message = bufferedReader.readLine();
            long t1 = System.currentTimeMillis();
            System.out.printf("message {%s} received from server after %d msec \n",message,(t1-t0));
            clientSocket.close();
            sent = true;
            } catch (UnknownHostException e) {
            	System.out.println("[TCP Client] cannot open the socket with the server");
            } catch (IOException ex) {
                System.out.println("[TCP Client] cannot open the socket with the server");
            }
    	return sent;
    }
}
