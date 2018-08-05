package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.awt.geom.Point2D;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import messages.ClientMessage_ACK;
import messages.Message_Interface;
import messages.SensorState;
import messages.ServerMessage_Request_MeasurementData;
import messages.ServerMessage_Request_MeasurementHistory;
import sensor.SensorImpl;
import tcpClient.ClientManager;
import tcpClient.TCPclient;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class RunTest_ClientMessage_ACK {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	final String serverHostName = "localhost";
	double global_watchdog_scale_factor = 0.01;
	ComputeEngine_Runnable comp_engine_1 = null;
	Thread testThread_readMessages = null;
	Thread testThread_server = null;
	SensorImpl sensor = null;
	protected String softwareImageID = "Release 1";
	protected float[][] sensor_coordinates_array = { {1.0f, 1.0f}  }; 
	boolean[] input_1hWatchog_timestamp_table = {false};
	boolean[] input_24hWatchog_timestamp_table = {false};
	
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
	
	String[] testPurpose = { "Verify that the run() function does not update the request_measurement_data flag to true if Global_1h_Watchdog time left to expiration is higher than Watchdog_Thresholds.HIGH. \nVerify also that in this case the close_ComputeEngine_Runnable flag is set to true",
							 "Verify that the run() function updates the request_measurement_data flag to true if ClientMessage_ACK has been received when Global_1h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGH.",
							 "Verify that the run() function updates both Local_1h_watchdog and Local_24h_watchdog based on Global_1h_Watchdog time left to expiration by setting some delay \nin order to synchronize Locla_1h_Watchdogs for multiple sensor that are running in parallel threads if ClientMessage_ACK has been received when Global_1h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGH.",
							 "Verify that the run() function does not update the request_measurement_data flag to true while it sets the close_ComputeEngine_Runnable flag to true if ClientMessage_ACK has been received when Global_1h_Watchdog has expired",
							 "Verify that the run() function does not update the request_measurement_history flag to true if Global_24h_Watchdog time left to expiration is higher than Watchdog_Thresholds.HIGHEST. \nVerify also that in this case the close_ComputeEngine_Runnable flag is set to true and the TCP conenction is closed", 
							 "Verify that the run() function updates the request_measurement_history flag to true if ClientMessage_ACK has been received when Global_24h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGHEST.",
							 "Verify that the run() function updates Local_24h_watchdog based on Global_24h_Watchdog time left to expiration by setting some delay \nin order to synchronize Local_24h_Watchdogs for multiple sensor that are running in parallel threads if ClientMessage_ACK has been received when Global_24h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGHEST.",
							 "Verify that the run() function does not update the request_measurement_history flag to true while it sets the close_ComputeEngine_Runnable flag to true if ClientMessage_ACK has been received when Global_24h_Watchdog has expired",
	 						 "Verify that the run() function responds to ClientMessage_ACK as follows: \n1) with ServerMessage_Request_MeasurementData if request_measurement_data is set to true, \n 2) with ServerMessage_Request_MeasurementHistory if request_measurement_history is set to true, \n 3) does not respond and sets the isComputeEngine_Runnable_running to false if close_ComputeEngine_Runnable is set to true"};
	
	
	static int testID = 1;
	
	public static void incrementTestID() {
		RunTest_ClientMessage_ACK.testID += 1;
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
		sensor.setSensorState(SensorState.OPERATIONAL);
		TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[sensor_ID_1-1], sensor.getSensorID()-1);
		TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[sensor_ID_1-1], sensor.getSensorID()-1);
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
				} catch (EOFException e) {
					// DO Nothing since it is unable to prevent to throw this exception when Client Manager is either a Mock or a Spy
				} catch (IOException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					System.out.println(e.getMessage());
					e.printStackTrace();
					assertTrue(false);

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
		
		System.out.println("\t\tTest Run "+RunTest_ClientMessage_ACK.testID+" Purpose:");
		System.out.println(testPurpose[(RunTest_ClientMessage_ACK.testID-1)]);
		System.out.println("\t\tTest Run "+RunTest_ClientMessage_ACK.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that the run() function does not update the request_measurement_data flag to true if Global_1h_Watchdog time left to expiration
	  								is higher than Watchdog_Thresholds.HIGH. Verify also that in this case the close_ComputeEngine_Runnable flag is set to true
	 * Local variables TBV:			request_measurement_data, close_ComputeEngine_Runnable
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
		
		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertFalse(receivedMessage instanceof ServerMessage_Request_MeasurementData);
		assertFalse(comp_engine_1.isComputeEngine_Runnable_running());
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that the run() function updates the request_measurement_data flag to true if ClientMessage_ACK has been received
	  								when Global_1h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGH
	 * Local variables TBV:			request_measurement_data
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(120 * global_watchdog_scale_factor);

		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(200);
		}
		
		assertTrue(receivedMessage instanceof ServerMessage_Request_MeasurementData);
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that the run() function updates both Local_1h_watchdog and Local_24h_watchdog based on Global_1h_Watchdog time left to expiration
	 								by setting some delay in order to synchronize Locla_1h_Watchdogs for multiple sensor that are running in parallel threads
	 								if ClientMessage_ACK has been received when Global_1h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGH.",
	 * Internal variables TBV:		local_1h_watchdog, local_24h_watchdog
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(120 * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(300 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);

		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);
		
		for (int measurements = 0; measurements < sensor.getSensor_m_history_array_size() - 2; measurements++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
			TCPserver.processing_engine.updateServerSensorList(sensor);
		}
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(120 * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(200 * global_watchdog_scale_factor);
		double expected_Local_24h_watchdog_after_22_messages = 300 * global_watchdog_scale_factor;
		double expected_Local_1h_watchdog_after_22_messages = 120 * global_watchdog_scale_factor * 0.75;
		
		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(100);
		}
		
		assertEquals(expected_Local_1h_watchdog_after_22_messages,		comp_engine_1.getLocal_1h_watchdog(), 0.1);	
		assertEquals(expected_Local_24h_watchdog_after_22_messages,		comp_engine_1.getLocal_24h_watchdog(), 0.1);
		assertTrue(receivedMessage instanceof ServerMessage_Request_MeasurementData);

		double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
		double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
		int humidity = ThreadLocalRandom.current().nextInt(0, 101);
		int temperature = ThreadLocalRandom.current().nextInt(0, 30);
		int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
		sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
		TCPserver.processing_engine.updateServerSensorList(sensor);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(120 * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(200 * global_watchdog_scale_factor);
		double expected_Local_24h_watchdog_after_23_messages = 200 * global_watchdog_scale_factor;
		double expected_Local_1h_watchdog_after_23_messages = 120 * global_watchdog_scale_factor * 0.5;
		
		comp_engine_1.setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		
		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(100);
		}
		
		assertEquals(expected_Local_1h_watchdog_after_23_messages,		comp_engine_1.getLocal_1h_watchdog(), 0.1);	
		assertEquals(expected_Local_24h_watchdog_after_23_messages,		comp_engine_1.getLocal_24h_watchdog(), 0.1);
		assertTrue(receivedMessage instanceof ServerMessage_Request_MeasurementData);
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that the run() function does not update the request_measurement_data flag to true while it sets the close_ComputeEngine_Runnable flag to true
	  								if ClientMessage_ACK has been received when Global_1h_Watchdog has expired
	 * Local variables TBV:			request_measurement_data, close_ComputeEngine_Runnable
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(0 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertFalse(receivedMessage instanceof ServerMessage_Request_MeasurementData);		
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_5
	 * Description: 				Verify that the run() function does not update the request_measurement_history flag to true if Global_24h_Watchdog time left to expiration
	  								is higher than Watchdog_Thresholds.HIGHEST Verify also that in this case the close_ComputeEngine_Runnable flag is set to true
	 * Local variables TBV:			request_measurement_history, close_ComputeEngine_Runnable
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException, InterruptedException {
		
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(1000 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(100);
		}
		
		assertFalse(receivedMessage instanceof ServerMessage_Request_MeasurementHistory);		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_6
	 * Description: 				Verify that the run() function updates the request_measurement_history flag to true if ClientMessage_ACK has been received
	  								when Global_24h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGHEST
	 * Local variables TBV:			request_measurement_history
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_6() throws IOException, InterruptedException {
		
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(200 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(200);
		}
		
		assertTrue(receivedMessage instanceof ServerMessage_Request_MeasurementHistory);
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_7
	 * Description: 				Verify that the run() function updates Local_24h_watchdog based on Global_24h_Watchdog time left to expiration by setting some delay
	 								in order to synchronize Local_24h_Watchdogs for multiple sensor that are running in parallel threads
	 								if ClientMessage_ACK has been received when Global_24h_Watchdog time left to expiration is lower than Watchdog_Thresholds.HIGHEST
	 * Internal variables TBV:		local_24h_watchdog
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_7() throws IOException, InterruptedException {
		
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(120 * global_watchdog_scale_factor);
		double expected_Local_24h_watchdog = 120 * global_watchdog_scale_factor * 0.5;
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(200);
		}
		
		assertEquals(expected_Local_24h_watchdog,		comp_engine_1.getLocal_24h_watchdog(), 0.1);	
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_8
	 * Description: 				Verify that the run() function does not update the request_measurement_history flag to true while it sets the close_ComputeEngine_Runnable flag to true
	 								if ClientMessage_ACK has been received when Global_24h_Watchdog has expired
	 * Local variables TBV:			request_measurement_history, close_ComputeEngine_Runnable
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_8() throws IOException, InterruptedException {
		
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(0 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(100);
		}
		
		assertFalse(receivedMessage instanceof ServerMessage_Request_MeasurementHistory);		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_9
	 * Description: 				"Verify that the run() function responds to ClientMessage_ACK as follows:
	  								1) with ServerMessage_Request_MeasurementData if request_measurement_data is set to true,
	  								2) with ServerMessage_Request_MeasurementHistory if request_measurement_history is set to true,
	  								3) does not respond and sets the isComputeEngine_Runnable_running to false if close_ComputeEngine_Runnable is set to true
	 * Internal variables TBV:		isComputeEngine_Runnable_running
	 * External variables TBV:		ClientMessage_ACK, ServerMessage_Request_MeasurementData, ServerMessage_Request_MeasurementHistory
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_9() throws IOException, InterruptedException {
		
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
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);

		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);
		
		for (int measurements = 0; measurements < sensor.getSensor_m_history_array_size() - 1; measurements++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
			TCPserver.processing_engine.updateServerSensorList(sensor);
		}
		
		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(100);
		}
		
		assertTrue(receivedMessage instanceof ServerMessage_Request_MeasurementData);
		Thread.sleep(50);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		comp_engine_1.setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		comp_engine_1.setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());

		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(200);
		}
		
		assertTrue(receivedMessage instanceof ServerMessage_Request_MeasurementHistory);		
		Thread.sleep(50);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getExpiration() * TCPserver.getMeasurements_limit() * global_watchdog_scale_factor);
		comp_engine_1.setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		comp_engine_1.setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		
		mockClientManager.sendMessage(new ClientMessage_ACK(sensor_ID_1), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		// wait until the test thread leaves the _1h_Watchdog_close_to_expire() function that contains a delay
		while (testThread_server.getState() == Thread.State.TIMED_WAITING) {
			Thread.sleep(100);
		}
		
		assertFalse(comp_engine_1.isComputeEngine_Runnable_running());
		Thread.sleep(50);
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+RunTest_ClientMessage_ACK.testID+" teardown section:");
	   
	   if(mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if(Global_1h_Watchdog.getInstance().getEnabled()) {
		   Global_1h_Watchdog.getInstance().setEnabled(false);
		   Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor);

	   }
	   if(Global_24h_Watchdog.getInstance().getEnabled()) {
		   Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor * TCPserver.getMeasurements_limit());
		   Global_24h_Watchdog.getInstance().setEnabled(false);
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
