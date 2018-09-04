package tcpServer;

import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import messages.ClientMessage_BootUp;
import messages.Message_Interface;
import messages.ServerMessage_SensorInfoQuerry;
import sensor.SensorImpl;
import tcpClient.ClientManager;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;;

public class StartServerTest {
	
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	TCPserver tcpserver_2 = null;
	ServerSocket serverSocket_1 = null;
	ServerSocket serverSocket_2 = null;
	Socket clientSocket = null;
	ClientManager mockClinetManagerTest = null;
	TCPserver mockTCPServerTest = null;
	final String serverHostName = "localhost";
	SensorImpl sensor = null;
	int sensor_ID_1 = 1;
	int number_of_sensors = 1;
	int measurements_limit = 24;
	
	String[] testPurpose = { 	"Verify that once the startServer function is called, there is a new server thread created",
								"Verify that once the startServer function is called, the server thread starts and it has the RUNNABLE thread state. Verify also that once the server socket is closed, the server thread state changes to the TERMINATED thread state",
								"Verify that there is no ThreadPoolExecutor started for processing client messages unless there has been any output stream for the client socket created",
								"Verify that once an output stream for the client socket is created, ThreadPoolExecutor is executed that launches the runnable ComputeEngine_Runnable class that handles communication via a TCP connection with sensors",
								"Verify that once an output stream for the client socket is created, all messages sent via this output stream are being handled in separated ThreadPoolExecutors",
								"Verify that there is a possibility to create up to 8 ThreadPoolExecutors to process client messages"};
	static int testID = 1;
	
	public static void incrementTestID() {
		StartServerTest.testID += 1;
	}	
	
	@Before
	public void before() throws IOException, ClassNotFoundException {
		
		tcpserver_1 = new TCPserver();
		serverSocket_1 = new ServerSocket();
		
		TCPserver.getInstance(port_1, number_of_sensors, measurements_limit, TCPserver.getWatchdogs_scale_factor());
		TCPserver.processing_engine = new ComputeEngine_Processing();
		sensor = new SensorImpl(sensor_ID_1);
		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
		
		if(StartServerTest.testID == 3) {
			mockClinetManagerTest = mock(ClientManager.class);
			mockTCPServerTest = mock(TCPserver.class);
		}
		if(StartServerTest.testID > 3) {
			mockClinetManagerTest = mock(ClientManager.class);
		}
		System.out.println("\t\tTest Run "+StartServerTest.testID+" Purpose:");
		System.out.println(testPurpose[(StartServerTest.testID-1)]);
		System.out.println("\t\tTest Run "+StartServerTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the startServer function is called, there is a new server thread created
	 * Internal variables TBV: 	serverThread
     * Exceptions thrown:		IOException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException {
		
		serverSocket_1.setReuseAddress(true);
	    serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    TCPserver.set_ServerRunning(true);

		assertEquals(null, 		tcpserver_1.getServerThread());
		
		tcpserver_1.startServer(serverSocket_1);
		assertNotEquals(null, 	tcpserver_1.getServerThread());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the startServer function is called, the server thread starts and it has the RUNNABLE thread state. 
	 	 						Verify also that once the server socket is closed, the server thread state changes to the TERMINATED thread state
	 * Internal variables TBV: 	serverThread
     * Exceptions thrown:		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));    
	    TCPserver.set_ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);
		
		assertEquals(Thread.State.RUNNABLE,		tcpserver_1.getServerThread().getState());
		
		serverSocket_1.close();
		Thread.sleep(100);
		
		assertEquals(Thread.State.TERMINATED,	tcpserver_1.getServerThread().getState());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that there is no ThreadPoolExecutor started for processing client messages unless there has been any output stream for the client socket created
	 * Internal variables TBV: 	serverProcessingPool
     * Exceptions thrown:		IOException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		TCPserver.set_ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);
		clientSocket = new Socket(serverHostName, port_1);
		
		assertEquals(0,	tcpserver_1.getThreadPoolExecutor().getActiveCount());

	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once an output stream for the client socket is created, ThreadPoolExecutor is executed that launches
	  						 	the runnable ComputeEngine_Runnable class that handles communication via a TCP connection with sensors
	 * Internal variables TBV: 	serverProcessingPool
	 * External variables TBV: 	ComputeEngine_Runnable.isComputeEngine_Runnable_running
	 * Mocked method calls:		ClientManager.getOutputStream, ClientManager.getInputReaderStream 
     * Exceptions thrown:		IOException, InterruptedException, ClassNotFoundException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException, ClassNotFoundException {
		
		int temp_sens_ID = 1;
		TCPserver.Server_Sensors_LIST.add(new SensorImpl(temp_sens_ID));
		TCPserver.processing_engine = new ComputeEngine_Processing();
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		TCPserver.set_ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);			
		clientSocket = new Socket(serverHostName, port_1);
		
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new ObjectOutputStream(clientSocket.getOutputStream()));
	        when(mockClinetManagerTest.getInputReaderStream()).thenReturn(new ObjectInputStream(clientSocket.getInputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		Thread.sleep(100);
		
		mockClinetManagerTest.getOutputStream().writeObject(new ClientMessage_BootUp(temp_sens_ID));
		Message_Interface receivedMessage = null;
		
		receivedMessage = (Message_Interface) mockClinetManagerTest.getInputReaderStream().readObject();
		
		// The server messages can be solely sent by the runnable ComputeEngine_Runnable class, hence the below statement gives an evidence that ThreadPoolExecutor launched ComputeEngine_Runnable
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoQuerry);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that once an output stream for the client socket is created, all messages sent via this output stream are being handled in separated ThreadPoolExecutors
	 * Internal variables TBV: 	serverProcessingPool
	 * Mocked method calls:	 	ClientManager.getOutputStream
     * Exceptions thrown:		IOException, InterruptedException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException, InterruptedException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		TCPserver.set_ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);			
		clientSocket = new Socket(serverHostName, port_1);
		
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new ObjectOutputStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		Thread.sleep(100);
		
		assertEquals(1,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
		
		clientSocket = new Socket(serverHostName, port_1);
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new ObjectOutputStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		Thread.sleep(100);
		
		assertEquals(2,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that there is a possibility to create up to 8 ThreadPoolExecutors to process client messages
	 * Internal variables TBV: 	serverProcessingPool
	 * Mocked method calls: 	ClientManager.getOutputStream
     * Exceptions thrown:		IOException, InterruptedException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_6() throws IOException, InterruptedException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		TCPserver.set_ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);			
		clientSocket = new Socket(serverHostName, port_1);
		
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new ObjectOutputStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		Thread.sleep(100);
		
		assertEquals(1,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
		
		for (int i = 2; i < 9 ; i++) {
			clientSocket = new Socket(serverHostName, port_1);
		    try {
		        when(mockClinetManagerTest.getOutputStream()).thenReturn(new ObjectOutputStream(clientSocket.getOutputStream()));
			} catch (EOFException e) {
				// DO Nothing since it is unable to prevent to throw this exception when Client Manager is either a Mock or a Spy
		    } catch (IOException e) {
		        fail(e.getMessage());
		    }
			Thread.sleep(100);
		}
		
		assertEquals(8,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
		
		clientSocket = new Socket(serverHostName, port_1);
		
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new ObjectOutputStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		Thread.sleep(100);
		
		assertEquals(8,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
	}

	@After
	public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+StartServerTest.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(tcpserver_1);
	   
	   if(serverSocket_1 != null){
		   if(serverSocket_1.isBound()) {
			   serverSocket_1.close();
		   }
	   }
	   if(serverSocket_2 != null){
		   if(serverSocket_2.isBound()) {
			   serverSocket_2.close();
		   }
	   }
	   
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   incrementTestID();
	}


}
