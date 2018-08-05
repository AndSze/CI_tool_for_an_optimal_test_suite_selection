package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import messages.ClientMessage_ACK;
import messages.ClientMessage_BootUp;
import messages.ClientMessage_SensorInfo;
import messages.Message_Interface;
import messages.ServerMessage_SensorInfoQuerry;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.SensorImpl;
import tcpClient.ClientManager;
import tcpClient.TCPclient;

public class RunTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	int sensor_ID_2 = 2;
	final String serverHostName = "localhost";
	double global_watchdog_scale_factor = 0.01;
	ComputeEngine_Runnable comp_engine_1 = null;
	Thread testThread_readMessages = null;
	Thread testThread_server = null;
	SensorImpl sensor = null;
	protected String softwareImageID = "Release 1";
	protected float[][] sensor_coordinates_array = { {1.0f, 1.0f}  }; 
	
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	
	// Socket that are real object used for the TCP connection purposes
	ServerSocket tempServerSocket_1 = null;
	Socket tempClientSocket_1 = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement, this socket is used in ComputeEngine_Runnable class instance
	Socket mock_CER_ClientSocket = null;
	
	// to mock Client Manager class instance
	ClientManager mockClientManager = null;
	ObjectOutputStream obj_out_stream = null;
	ObjectInputStream obj_in_stream = null;
	
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
	
	// to mock TCPclient class instance
	TCPclient mockTCPclient = null;
		
	// to mock server threads
	Thread mockServerThread = null;
	
	String[] testPurpose = { "Verify that once the run() function is called while the isComputeEngine_Runnable_running flag is equal to true, the ComputeEngine_Runnable class instance that runs the run() function is able to read messages send from TCPserver",
							 "Verify that once the run() function is called while the isComputeEngine_Runnable_running flag is equal to false, the ComputeEngine_Runnable class instance that runs the run() function is NOT able to read messages send from TCPserver",
							 "Verify that once the run() function is called, it hangs in the readMessage() function until it gets a new message from TCPsclient. \nIt is verified also that the state machine of run() function is executed for all messages received from TCPclient, but every time the currently processing message is different from the previous",
							 "Verify that once the run() function receives any message from TCPclinet that has a sensor ID that is not defined for any sensor in Server_Sensors_LIST, \nthe state machine of run() function ignores this message and waits for a next message"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		RunTest.testID += 1;
	}

	@Before
	public void before() throws IOException, ClassNotFoundException {
		
		// mocked objects 
		mockTCPserverTest = mock(TCPserver.class);
		mock_CER_ClientSocket = mock(Socket.class);
		mockClientManager = Mockito.spy(ClientManager.class);
		mockTCPclient = mock(TCPclient.class);
		
		// create a real Server Socket for the TCPserver mock to enable the Client Socket to set up the TCP connection
		tempServerSocket_1 = new ServerSocket();
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		
		TCPserver.processing_engine = new ComputeEngine_Processing();
		sensor = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
		
		/* To avoid "remote deadlock" - there is a need to submit mockComputeEngine_Runnable to ThreadPoolExecutor 
		 * The ObjectInputStream on the client is waiting for the object stream from the server before proceeding, but the server isn't going to send that, 
		 * because its ObjectInputStream is waiting for the header from the client before proceeding... 
		 */
		
		Mockito.doAnswer(new Answer<Thread>() {
            @Override
            public Thread answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 0 && arguments[0] != null ) {
                	final ServerSocket servSocket = (ServerSocket) arguments[0];
                	mockServerThread = new Thread(new Runnable() {
                		public void run() {
                			while(!servSocket.isClosed()) {
		                		try {
		                			mock_CER_ClientSocket = servSocket.accept();
		                			System.out.println("Server Thread Started.");
								} catch (IOException IOex) {
									mockServerThread.interrupt();
									System.out.println("Server Thread Stopped.");
									System.out.println("Server" + IOex.getMessage());
									break;
								}
                			}
                		}
                	});
                }
                return mockServerThread;
            }
		}).when(mockTCPserverTest).startServer(Matchers.any(ServerSocket.class));

		// Mockito.doAnswer - to mock void method to do something (mock the behavior despite being void) - in this case it is used for TCPserver.startServer();
		// the test uses this approach for the purpose of avoiding actual messages sent via TCP - it will be checked in the integration tests
		
		// test thread that listens for messages on the client side
		testThread_readMessages = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					
					// create ObjectInputStream on the client to once a ComputeEngine_Runnable class instance is created
					obj_in_stream = new ObjectInputStream(mockTCPclient.getClientSocket().getInputStream());
					when(mockClientManager.getInputReaderStream()).thenReturn(obj_in_stream);
					
					while(comp_engine_1.get_ComputeEngineRunning()) {
						receivedMessage = (Message_Interface) mockClientManager.readMessage(mockClientManager.getInputReaderStream());
					}
				} catch (ClassNotFoundException e) {
					// To prove that exception's stack trace reported by JUnit caught ClassNotFoundException
					assertTrue(false);
					e.printStackTrace();
				} catch (IOException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					assertTrue(false);
					e.printStackTrace();
				}
			}
		});
		
		// test thread that listens for messages on the server side and resends particular responses for the received messages
		testThread_server = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				comp_engine_1.run();
			}
		});
		
		System.out.println("\t\tTest Run "+RunTest.testID+" Purpose:");
		System.out.println(testPurpose[(RunTest.testID-1)]);
		System.out.println("\t\tTest Run "+RunTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that once the run() function is called while the isComputeEngine_Runnable_running flag is equal to true, 
	 								the ComputeEngine_Runnable class instance that runs the run() function is able to read messages send from TCPserver
	 * Internal variables TBV:		inputStream, outputStream, isComputeEngine_Runnable_running
	 * External variables TBV:		ServerMessage_SensorInfoQuerry, ClientMessage_BootUp, ClientMessage_ACK
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);
		
		mockClientManager.sendMessage(new ClientMessage_BootUp(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoQuerry);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that once the run() function is called while the isComputeEngine_Runnable_running flag is equal to false, 
	 								the ComputeEngine_Runnable class instance that runs the run() function is NOT able to read messages send from TCPserver
	 * Internal variables TBV:		inputStream, outputStream, isComputeEngine_Runnable_running
	 * External variables TBV:		ServerMessage_SensorInfoQuerry, ClientMessage_BootUp, ClientMessage_ACK, ClientMessage_SensorInfo, ServerMessage_SensorInfoUpdate
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);

		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);
		
		assertTrue(comp_engine_1.isComputeEngine_Runnable_running());
		
		mockClientManager.sendMessage(new ClientMessage_BootUp(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoQuerry);
		
		SensorImpl temp_client_sensor = new SensorImpl(sensor_ID_1);
		
		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_client_sensor), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoUpdate);
		
		// send ClientMessage_ACK message - it is required to set compute engine runnable to false
		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertFalse(comp_engine_1.isComputeEngine_Runnable_running());
		
		mockClientManager.sendMessage(new ClientMessage_BootUp(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertFalse(receivedMessage instanceof ServerMessage_SensorInfoQuerry);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that once the run() function is called, it hangs in the readMessage() function until it gets a new message from TCPsclient.
	 								It is verified also that the state machine of run() function is executed for all messages received from TCPclient,
	 						 		but every time the currently processing message is different from the previous
	 * Internal variables TBV:		inputStream, outputStream
	 * External variables TBV:		ClientMessage_BootUp, ClientMessage_ACK
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);

		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);
		
		Message_Interface receivedMessage_old = null;
		Message_Interface receivedMessage_new = null;
			
		assertEquals(null, 							receivedMessage);
		
		mockClientManager.sendMessage(new ClientMessage_BootUp(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_old = receivedMessage;
		
		mockClientManager.sendMessage(new ClientMessage_BootUp(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_new = receivedMessage;
		
		assertNotEquals(receivedMessage_new, 		receivedMessage_old);
	}
	
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that once the run() function receives any message from TCPclinet that has a sensor ID that is not defined for any sensor in Server_Sensors_LIST,
	 								the state machine of run() function ignores this message and waits for a next message
	 * Internal variables TBV:		inputStream, outputStream, isComputeEngine_Runnable_running
	 * External variables TBV:		ClientMessage_SensorInfo, ServerMessage_SensorInfoUpdate, ClientMessage_ACK, SensorImpl.sensorID, Server_Sensors_LIST
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);

		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);
		
		SensorImpl temp_client_sensor = new SensorImpl(sensor_ID_2);
		
		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_client_sensor), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertFalse(receivedMessage instanceof ServerMessage_SensorInfoUpdate);
		assertEquals(null, 				receivedMessage);
		assertTrue(comp_engine_1.isComputeEngine_Runnable_running());
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+RunTest.testID+" teardown section:");
	   
	   if(mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if(testThread_readMessages != null) {
		   if (testThread_readMessages.isAlive()) {
			   testThread_readMessages.interrupt();
		   }
	   }
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}

}
