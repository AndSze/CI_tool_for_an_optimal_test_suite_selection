package tcpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ClientManager implements TCPclient_interface{

	protected PrintStream outputStream = null;
	protected InputStreamReader inputStream = null;
	
	protected ClientManager(PrintStream outputStream, InputStreamReader inputStream) throws ClassNotFoundException{
		this.outputStream = outputStream;
        this.inputStream = inputStream;
	}
	
	public ClientManager() {
		super();
	}
	
	public void initClientManager(Socket clientSocket) {
		try {
		System.out.println("Client Manager created");
		outputStream = new PrintStream(clientSocket.getOutputStream());
        inputStream = new InputStreamReader(clientSocket.getInputStream());
        new ClientManager(outputStream, inputStream);
		} catch (IOException IOEx) {
	    	System.out.println("Error: The client manager cannot be created for output Steam: "+ outputStream+" and input Steam: "+ inputStream);
	    	IOEx.printStackTrace();
		} catch (ClassNotFoundException CNFex) {
			//will be executed when the server cannot be created
			System.out.println("Error: Application tries to load in a class through its string name using "+clientSocket.getClass().getName()+" ,but no definition for the class with the specified name could be found.");
			CNFex.printStackTrace();
		}
	}

	public long sendMessage(String message, Socket clientSocket) throws IOException {
        long t0 = System.currentTimeMillis();
        // it activates serverSocket.accept() on the server side
        outputStream.print(message); 
        return (t0);
	}
	
	public void receiveMessage(long t0, Socket clientSocket, int numberOfMsgsSent, int numberOfMsgsReceived) throws IOException, InterruptedException {
		BufferedReader bufferedReader = new BufferedReader(inputStream);
		String message;
		int timeout = 0;
		
		while(true)
        {
			if(bufferedReader.ready())
			{
				message = bufferedReader.readLine();
	    		//String message = bufferedReader.readLine();
		        long t1 = System.currentTimeMillis();
		        System.out.printf("message {%s} received from server after %d msec \n",message,(t1-t0));
		        //Thread.sleep(10);
		        break;
			}
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
	
	public PrintStream getOutputStream() {
		return this.outputStream;
	}

	public void setOutputStream(PrintStream outputStream) {
		this.outputStream = outputStream;
	}
	
	public InputStreamReader getInputStream() {
		return this.inputStream;
	}

	public void setInputStream(InputStreamReader inputStream) {
		this.inputStream = inputStream;
	}


}
