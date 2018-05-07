package tcpClient;

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

public class ClientManager implements TCPclient_interface{

	private PrintStream outputStream = null;
	private InputStreamReader inputStream = null;
	
	// default constructor 
	public ClientManager() {
		super();
	}
	
	// overloaded constructor
	private ClientManager(PrintStream outputStream, InputStreamReader inputStream){
		this.outputStream = outputStream;
        this.inputStream = inputStream;
	}

	
	public ClientManager initClientManager(Socket clientSocket) throws IOException{
	
			outputStream = new PrintStream(clientSocket.getOutputStream());
	        inputStream = new InputStreamReader(clientSocket.getInputStream());
	        return (new ClientManager(outputStream, inputStream));
	       
	}

	public long sendMessage(String message, Socket clientSocket) throws IOException {
		
        long t0 = System.currentTimeMillis();
        // it activates serverSocket.accept() on the server side
        outputStream.print(message); 
        return (t0);
        
	}
	
	public ReceivedMessage receiveMessage(long t0, Socket clientSocket) throws IOException {
		
		BufferedReader bufferedReader = new BufferedReader(inputStream);
		ReceivedMessage receivedMessage = null;
		String message = null;
		long t1 = 0;
		
		while(true)
        {
			if(bufferedReader.ready())
			{
				message = bufferedReader.readLine();
				t1 = System.currentTimeMillis();
				receivedMessage = new ReceivedMessage(message, t1);
	    		//String message = bufferedReader.readLine();
		        //Thread.sleep(10);
		        break;
			}
        }
		return receivedMessage;
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
	
	public PrintStream getOutputStream() {
		return this.outputStream;
	}

	public InputStreamReader getInputReaderStream() {
		return this.inputStream;
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
