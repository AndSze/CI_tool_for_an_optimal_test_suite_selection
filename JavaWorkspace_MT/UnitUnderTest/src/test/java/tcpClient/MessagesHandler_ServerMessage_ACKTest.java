package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import messages.ClientMessage_ACK;
import messages.ClientMessage_MeasurementData;
import messages.Message_Interface;
import messages.SensorState;
import messages.ServerMessage_ACK;
import messages.ServerMessage_Request_MeasurementData;
import messages.ServerMessage_Request_MeasurementHistory;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.SensorImpl;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;

public class MessagesHandler_ServerMessage_ACKTest {

	ClientManager clientManager_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    String serverHostName  = "localhost";
    Thread testThread_server = null;
    Thread testThread_client = null;
    SensorImpl temp_sens_sent_1 = null; 
    
    // Client Socket for the TCPclient class mock
	Socket TCPclientSocket = null;
	
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
    
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket_1 = null;
	TCPclient mockTCPclientTest = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	
	String[] testPurpose = { "Verify that the messagesHandler() function responds to ServerMessage_ACK with ClientMessage_ACK",
							 "Verify that the messagesHandler() sets the isClientManagerRunning flag to false, synchronizes Local_1h_Watchdog.timeLeftBeforeExpiration with Global_1h_Watchdog.timeLeftBeforeExpiration received in ServerMessage_ACK and enables Local_1h_Watchdog if ServerMessage_ACK was received when the wait_for_measurement_history flag is set to false",
							 "Verify that the messagesHandler() disables Local_1h_Watchdog if ServerMessage_ACK was received when the wait_for_measurement_history flag is set to true"};
	static int testID = 1;
	
	public static void incrementTestID() {
		MessagesHandler_ServerMessage_ACKTest.testID += 1;
	}
	
	@SuppressWarnings("static-access")
	@Before
	public void before() throws IOException {
		
		// call the default constructor for the ClientManager class to create an instance: clientManager_1
		clientManager_1 = new ClientManager();
		
    	if (mockTCPclientTest.Client_Sensors_LIST == null) {
    		mockTCPclientTest.Client_Sensors_LIST = new ArrayList<>();
    	}
    	if (mockTCPclientTest.searchInClientSensorList(sensor_ID_1) == null) {
    		mockTCPclientTest.Client_Sensors_LIST = mockTCPclientTest.updateClientSensorList(new SensorImpl(sensor_ID_1));
    	}
		// mocked objects 
		mockTCPserverTest = mock(TCPserver.class);
		mockTCPclientTest = mock(TCPclient.class);
		mockClientSocket = mock(Socket.class);
		mockComputeEngine_Runnable = mock(ComputeEngine_Runnable.class);
		
		// create a real Server Socket for the TCPserver mock to enable the Client Socket to set up the TCP connection		
		tempServerSocket_1 = new ServerSocket();
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		
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
									mockClientSocket = servSocket.accept();
									mockComputeEngine_Runnable = Mockito.spy(new ComputeEngine_Runnable(mockClientSocket, 1.0, false));
									auxiliaryServerThreadExecutor.submit(mockComputeEngine_Runnable);
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
		
		// test thread that listens for messages on the server side
		testThread_server = new Thread(new Runnable() {
		//Runnable serverTask = new Runnable() {
		public void run() {
				try {
					while(clientManager_1.isClientManagerRunning()) {
						receivedMessage = (Message_Interface) (mockComputeEngine_Runnable.readMessage(mockComputeEngine_Runnable.getInputReaderStream()));
					}
				} catch (ClassNotFoundException e) {
					// To prove that exception's stack trace reported by JUnit caught ClassNotFoundException
					e.printStackTrace();
				} catch (IOException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					e.printStackTrace();
				}
			}
		});
		
		// test thread that listens for messages on the client side and resends particular responses for the received messages
		testThread_client = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				clientManager_1.messagesHandler(clientManager_1.getOutputStream(), clientManager_1.getInputReaderStream());
			}
		});
		
		int sensorID = 1;
		float[][] sensor_coordinates_array_1 = {{11.0f, 14.0f}};
		String softwareImageID_1 = "Release X";
		int measurement_limit_1 = 5;
		SensorState sens_state_1 = SensorState.MAINTENANCE;
		
		temp_sens_sent_1 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_1[0][0], sensor_coordinates_array_1[0][1]), softwareImageID_1, measurement_limit_1);
		temp_sens_sent_1.setSensorState(sens_state_1);
	
		System.out.println("\t\tTest Run "+MessagesHandler_ServerMessage_ACKTest.testID+" Purpose:");
		System.out.println(testPurpose[(MessagesHandler_ServerMessage_ACKTest.testID-1)]);
		System.out.println("\t\tTest Run "+MessagesHandler_ServerMessage_ACKTest.testID+" Logic:");
	}

   /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that the messagesHandler() function responds to ServerMessage_ACK with ClientMessage_ACK
	 * External variables TBV:		ClientMessage_ACK, ServerMessage_ACK
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(10);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(10);
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(10);
			
		assertTrue(receivedMessage instanceof ClientMessage_ACK);
	}
	
   /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that the messagesHandler() sets the isClientManagerRunning flag to false, 
	 								synchronizes Local_1h_Watchdog.timeLeftBeforeExpiration with Global_1h_Watchdog.timeLeftBeforeExpiration received in ServerMessage_ACK
	 								and enables Local_1h_Watchdog if ServerMessage_ACK was received when the wait_for_measurement_history flag is set to false
	 * Internal variables TBV:		isClientManagerRunning
	 * External variables TBV:		ClientMessage_ACK, ServerMessage_ACK, Local_1h_Watchdog.isPaused, Local_1h_Watchdog.millisecondsLeftUntilExpiration
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(10);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(10);
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(10);
			
		assertTrue(receivedMessage instanceof ClientMessage_ACK);
		
		assertEquals(mockComputeEngine_Runnable.getLocal_1h_watchdog(), 			Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.5);
		assertTrue(Local_1h_Watchdog.getInstance().getEnabled());
		assertFalse(clientManager_1.isClientManagerRunning());
	}
	
   /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that the messagesHandler() disables Local_1h_Watchdog if ServerMessage_ACK was received when the wait_for_measurement_history flag is set to true
	 * External variables TBV:		ServerMessage_ACK, Local_1h_Watchdog.isPaused	
	 * Local variables TBV:			wait_for_measurement_data
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(10);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(10);
		
		double sens_watchdog_scale_factor_1 = 0.01;
		double received_Local_1h_watchdog = 120 * sens_watchdog_scale_factor_1;
		when(mockComputeEngine_Runnable.getLocal_1h_watchdog()).thenReturn(received_Local_1h_watchdog);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_1);
		
		for (int measurements = 0; measurements < temp_sens_sent_1.getSensor_m_history_array_size(); measurements++) {
		
			mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
													temp_sens_sent_1.getSensorState(),
													mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
													temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
			Thread.sleep(50);
			
			// change sensor state to SensorState.OPERATIONAL what will result in setting wait_for_measurement_data to true in the state machine statement that processes the next ServerMessage_SensorInfoUpdate
			SensorState sens_state_2 = SensorState.OPERATIONAL;
			temp_sens_sent_1.setSensorState(sens_state_2);
			
			mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
													temp_sens_sent_1.getSensorState(),
													mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
													temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
			Thread.sleep(50);
			
			mockComputeEngine_Runnable.sendMessage(new ServerMessage_Request_MeasurementData(sensor_ID_1), mockComputeEngine_Runnable.getOutputStream());
			Thread.sleep(50);
			
			assertTrue(receivedMessage instanceof ClientMessage_MeasurementData);
		}
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(50);
			
		assertTrue(receivedMessage instanceof ClientMessage_ACK);
		
		assertFalse(Local_1h_Watchdog.getInstance().getEnabled());
		
		// send ServerMessage_Request_MeasurementHistory to enable TCP connection to be closed upon receiving ServerMessage_ACK
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_Request_MeasurementHistory(sensor_ID_1), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(50);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()) , mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(50);
	}
	
	@SuppressWarnings("static-access")
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+MessagesHandler_ServerMessage_ACKTest.testID+" teardown section:");
	   
	   if (clientManager_1.getInputReaderStream() != null) {
		   clientManager_1.closeInStream();
	   }
	   if (clientManager_1.getOutputStream() != null){
		   clientManager_1.closeOutStream();
	   }
	   if(testThread_server != null) {
		   if (testThread_server.isAlive()) {
			   testThread_server.interrupt();
		   }
	   }
	   if(testThread_client != null) {
		   if (testThread_client.isAlive()) {
			   testThread_client.interrupt();
		   }
	   }
	   for (int index = 0; index < mockTCPclientTest.Client_Sensors_LIST.size(); index++) {
		   mockTCPclientTest.Client_Sensors_LIST.remove(index);
	   }
	   if(Local_1h_Watchdog.getInstance() != null) {
		   Local_1h_Watchdog.getInstance().setM_instance(null);
	   }
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}

}

