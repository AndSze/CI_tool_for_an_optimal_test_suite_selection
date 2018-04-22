package tcpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ClientManager implements TCPclient_interface{

	private PrintStream printStream;
	private InputStreamReader inputStream;
	
	public ClientManager(Socket clientSocket) throws IOException{
		printStream = new PrintStream(clientSocket.getOutputStream());
        inputStream = new InputStreamReader(clientSocket.getInputStream());
	}
	
	public long sendMessage(String message) {
        long t0 = System.currentTimeMillis();
        // it activates serverSocket.accept() on the server side
        printStream.print(message); 
        return (t0);
	}
	
	public void receiveMessage(long t0) throws IOException, InterruptedException {
		BufferedReader bufferedReader = new BufferedReader(inputStream);
		String message;
		int timeout = 0;
		
		while(bufferedReader.ready())
        {
			message = bufferedReader.readLine();
    		//String message = bufferedReader.readLine();
	        long t1 = System.currentTimeMillis();
	        System.out.printf("message {%s} received from server after %d msec \n",message,(t1-t0));
        }
		/*
		while(timeout < 5)
	    {
	    	if (bufferedReader.ready()) {
	
	    		message = bufferedReader.readLine();
	    		//String message = bufferedReader.readLine();
		        long t1 = System.currentTimeMillis();
		        System.out.printf("message {%s} received from server after %d msec \n",message,(t1-t0));
	    	}
	    	else
	    	{
	    		Thread.sleep(210);
	        	//System.out.println(timeout);
	    		timeout++;
	    	}
	    	
	    }
        System.out.println("Client while loop ended with timeout: " + timeout);
	       */
	}
	

	public void hadleIncomingMessage() {
		// TODO Auto-generated method stub
		
	}

	public void sendMessage() {
		// TODO Auto-generated method stub
		
	}

	public void triggerComputeEngine() {
		// TODO Auto-generated method stub
		
	}


}
