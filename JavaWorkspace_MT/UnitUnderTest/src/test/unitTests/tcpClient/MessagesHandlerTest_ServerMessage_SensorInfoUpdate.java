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
import messages.ClientMessage_BootUp;
import messages.Message_Interface;
import messages.SensorState;
import messages.ServerMessage_ACK;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.SensorImpl;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;


public class MessagesHandlerTest_ServerMessage_SensorInfoUpdate {

	ClientManager clientManager_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    int sensor_ID_2 = 2;
    String serverHostName  = "localhost";
    Thread testThread_server = null;
    Thread testThread_client = null;
    Thread testThread_exception = null;
    SensorImpl sensor_1 = null;
    
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
	
	String[] testPurpose = { "Verify that the messagesHandler() function responds to ServerMessage_SensorInfoUpdate with ClientMessage_BootUp if the received sensor state is either SensorState.MAINTENANCE or SensorState.PRE_OPERATIONAL.\nVerify also that the sensor instance in Client_Sensors_LIST is updated with the new settings a the new sensor state and the resetSensor() function is called.",
							 "Verify that the messagesHandler() function responds to ServerMessage_SensorInfoUpdate with ClientMessage_ACK if the received sensor state is SensorState.OPERATIONAL. \nVerify also that the sensor instance in Client_Sensors_LIST is updated with the received watchdog_scale_factor and sensor state.",
							 "Verify that the messagesHandler() function responds to ServerMessage_SensorInfoUpdate with ClientMessage_ACK if the received sensor state is SensorState.DEAD. \nVerify also that the sensor instance in Client_Sensors_LIST is updated with the sensor state and the isClientManagerRunning is set to false.",
							 "Verify that the messagesHandler() function updates watchdogs_scale_factor and measurements_limit of the TCPclient class instance regardless of the received sensor state in ServerMessage_SensorInfoUpdate. \nVerify also that Local_1h_Watchdog is enabled and its timeLeftBeforeExpiration is synchronized with Global_1h_Watchdog.timeLeftBeforeExpiration that was received in ServerMessage_SensorInfoUpdate.",
							 "Verify that the messagesHandler() function sets the is ClientManagerRunning flag to false to close the TCP connection if the sensor state is set to SensorState.OPERATIONAL \nand the received Global_1h_Watchdog.timeLeftBeforeExpiration is higher than Local_1h_Watchdog._1h_WatchdogExpiration divided by 4."};
					
	static int testID = 1;
	
	public static void incrementTestID() {
		MessagesHandlerTest_ServerMessage_SensorInfoUpdate.testID += 1;
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
		
		// there is not need to verify messages sent by the Client Manager in the last 2 test runs
		if(MessagesHandlerTest_ServerMessage_SensorInfoUpdate.testID < 4) {
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
		}
		
		// test thread that listens for messages on the client side and resends particular responses for the received messages
		testThread_client = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				clientManager_1.messagesHandler(clientManager_1.getOutputStream(), clientManager_1.getInputReaderStream());
			}
		});
		
		System.out.println("\t\tTest Run "+MessagesHandlerTest_ServerMessage_SensorInfoUpdate.testID+" Purpose:");
		System.out.println(testPurpose[(MessagesHandlerTest_ServerMessage_SensorInfoUpdate.testID-1)]);
		System.out.println("\t\tTest Run "+MessagesHandlerTest_ServerMessage_SensorInfoUpdate.testID+" Logic:");
	}
	
   /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that the messagesHandler() function responds to ServerMessage_SensorInfoUpdate with ClientMessage_BootUp if the received sensor state is 
	 								either SensorState.MAINTENANCE or SensorState.PRE_OPERATIONAL. Verify also that the sensor instance in Client_Sensors_LIST
	 								is updated with the new settings a the new sensor state and the resetSensor() function is called.
	 * External variables TBV:		ServerMessage_SensorInfoUpdate, ClientMessage_BootUp, SensorImpl.sensorID, SensorImpl.coordinates, SensorImpl.softwareImageID, 
	 								SensorImpl.sensorState, SensorImpl.sensor_m_history_array_size, SensorImpl.numberOfMeasurements, SensorImpl.sensor_watchdog_scale_factor
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		int sensorID = 1;
		float[][] sensor_coordinates_array_1 = {{18.0f, 15.0f}};
		String softwareImageID_1 = "Release XX";
		int measurement_limit_1 = 48;
		SensorState sens_state_1 = SensorState.MAINTENANCE;
		double sens_watchdog_scale_factor_1 = 0.25;
		
		SensorImpl temp_sens_sent_1 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_1[0][0], sensor_coordinates_array_1[0][1]), softwareImageID_1, measurement_limit_1);
		temp_sens_sent_1.setSensorState(sens_state_1);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_1);
		
		SensorImpl temp_sens_received_1 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertNotEquals(temp_sens_sent_1.getCoordinates(),					temp_sens_received_1.getCoordinates());
		assertNotEquals(temp_sens_sent_1.getLocal_watchdog_scale_factor(),	temp_sens_received_1.getLocal_watchdog_scale_factor(), 0.01);
		assertNotEquals(temp_sens_sent_1.getSoftwareImageID(),				temp_sens_received_1.getSoftwareImageID());
		assertNotEquals(temp_sens_sent_1.getSensor_m_history_array_size(),	temp_sens_received_1.getSensor_m_history_array_size());
		
		// add measurement to prove that the sensor has been reset (senor reset removes all measurement from a sensor's memory)
		temp_sens_sent_1.addMeasurement(1.0, 1.0, 1, 1, 1);
		int number_of_measurements = 1;
		assertEquals(number_of_measurements,  temp_sens_sent_1.getNumberOfMeasurements());
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
												temp_sens_sent_1.getSensorState(),
												mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
												temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		temp_sens_received_1 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertTrue(receivedMessage instanceof ClientMessage_BootUp);
		
		int number_of_measurements_after_reset = 0;
		assertNotEquals(number_of_measurements,  							temp_sens_received_1.getNumberOfMeasurements());
		assertEquals(number_of_measurements_after_reset,  					temp_sens_received_1.getNumberOfMeasurements());
		assertEquals(temp_sens_sent_1.getSensorState(),						temp_sens_sent_1.getSensorState());
		assertEquals(temp_sens_sent_1.getCoordinates(),						temp_sens_received_1.getCoordinates());
		assertEquals(temp_sens_sent_1.getLocal_watchdog_scale_factor(),		temp_sens_received_1.getLocal_watchdog_scale_factor(), 0.01);
		assertEquals(temp_sens_sent_1.getSensorID(),						temp_sens_received_1.getSensorID());
		assertEquals(temp_sens_sent_1.getSoftwareImageID(),					temp_sens_received_1.getSoftwareImageID());
		assertEquals(temp_sens_sent_1.getSensor_m_history_array_size(),		temp_sens_received_1.getSensor_m_history_array_size());
		
		float[][] sensor_coordinates_array_2 = {{18.0f, 13.0f}};
		String softwareImageID_2 = "Release XY";
		int measurement_limit_2 = 36;
		SensorState sens_state_2 = SensorState.PRE_OPERATIONAL;
		double sens_watchdog_scale_factor_2 = 0.18;
		
		SensorImpl temp_sens_sent_2 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_2[0][0], sensor_coordinates_array_2[0][1]), softwareImageID_2, measurement_limit_2);
		temp_sens_sent_1.setSensorState(sens_state_2);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_2);
		
		SensorImpl temp_sens_received_2 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertNotEquals(temp_sens_sent_2.getCoordinates(),					temp_sens_received_2.getCoordinates());
		assertNotEquals(temp_sens_sent_2.getSensorState(),					temp_sens_received_2.getSensorState());
		assertNotEquals(temp_sens_sent_2.getLocal_watchdog_scale_factor(),	temp_sens_received_2.getLocal_watchdog_scale_factor(), 0.01);
		assertNotEquals(temp_sens_sent_2.getSoftwareImageID(),				temp_sens_received_2.getSoftwareImageID());
		assertNotEquals(temp_sens_sent_2.getSensor_m_history_array_size(),	temp_sens_received_2.getSensor_m_history_array_size());
		
		// add measurement to prove that the sensor has been reset (senor reset removes all measurement from a sensor's memory)
		temp_sens_sent_2.addMeasurement(1.0, 1.0, 1, 1, 1);
		assertEquals(number_of_measurements,  temp_sens_sent_1.getNumberOfMeasurements());
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_2.getSensorID(), temp_sens_sent_2.getCoordinates(), temp_sens_sent_2.getSoftwareImageID(), 
												temp_sens_sent_2.getSensorState(),
												mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
												temp_sens_sent_2.getLocal_watchdog_scale_factor(), temp_sens_sent_2.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		temp_sens_received_2 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertTrue(receivedMessage instanceof ClientMessage_BootUp);
		
		assertNotEquals(number_of_measurements,  							temp_sens_received_2.getNumberOfMeasurements());
		assertEquals(number_of_measurements_after_reset,  					temp_sens_received_2.getNumberOfMeasurements());
		assertEquals(temp_sens_sent_2.getSensorState(),						temp_sens_received_2.getSensorState());
		assertEquals(temp_sens_sent_2.getCoordinates(),						temp_sens_received_2.getCoordinates());
		assertEquals(temp_sens_sent_2.getLocal_watchdog_scale_factor(),		temp_sens_received_2.getLocal_watchdog_scale_factor(), 0.01);
		assertEquals(temp_sens_sent_2.getSensorID(),						temp_sens_received_2.getSensorID());
		assertEquals(temp_sens_sent_2.getSoftwareImageID(),					temp_sens_received_2.getSoftwareImageID());
		assertEquals(temp_sens_sent_2.getSensor_m_history_array_size(),		temp_sens_received_2.getSensor_m_history_array_size());

		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensorID, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
	}
	/***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that the messagesHandler() function responds to ServerMessage_SensorInfoUpdate with ClientMessage_ACK if the received sensor state is 
	 								SensorState.OPERATIONAL. Verify also that the sensor instance in Client_Sensors_LIST is updated with the received watchdog_scale_factor and sensor state.
	 * External variables TBV:		ServerMessage_SensorInfoUpdate, ClientMessage_ACK, SensorImpl.sensorState, SensorImpl.sensor_watchdog_scale_factor
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		int sensorID = 1;
		float[][] sensor_coordinates_array_1 = {{11.0f, 14.0f}};
		String softwareImageID_1 = "Release X";
		int measurement_limit_1 = 12;
		SensorState sens_state_1 = SensorState.OPERATIONAL;
		double sens_watchdog_scale_factor_1 = 0.15;
		
		SensorImpl temp_sens_sent_1 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_1[0][0], sensor_coordinates_array_1[0][1]), softwareImageID_1, measurement_limit_1);
		temp_sens_sent_1.setSensorState(sens_state_1);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_1);
		
		SensorImpl temp_sens_received_1 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertNotEquals(temp_sens_sent_1.getLocal_watchdog_scale_factor(),	temp_sens_received_1.getLocal_watchdog_scale_factor(), 0.01);
		assertNotEquals(temp_sens_sent_1.getSensorState(),						temp_sens_received_1.getSensorState());
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
												temp_sens_sent_1.getSensorState(),
												mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
												temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		temp_sens_received_1 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertTrue(receivedMessage instanceof ClientMessage_ACK);

		assertEquals(temp_sens_sent_1.getLocal_watchdog_scale_factor(),		temp_sens_received_1.getLocal_watchdog_scale_factor(), 0.01);
		assertEquals(temp_sens_sent_1.getSensorState(),						temp_sens_received_1.getSensorState());
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensorID, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
	}	
	
	/***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that the messagesHandler() function responds to ServerMessage_SensorInfoUpdate with ClientMessage_ACK if the received sensor state is 
	 								SensorState.DEAD. Verify also that the sensor instance in Client_Sensors_LIST is updated with the sensor state and the isClientManagerRunning is set to false.
	 * Internal variables TBV:		inputStream, outputStream, isClientManagerRunning
	 * External variables TBV:		ServerMessage_SensorInfoUpdate, ClientMessage_ACK, SensorImpl.sensorState, SensorImpl.sensor_watchdog_scale_factor
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_3() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		int sensorID = 1;
		float[][] sensor_coordinates_array_1 = {{11.0f, 14.0f}};
		String softwareImageID_1 = "Release X";
		int measurement_limit_1 = 12;
		SensorState sens_state_1 = SensorState.DEAD;
		double sens_watchdog_scale_factor_1 = 0.15;
		
		SensorImpl temp_sens_sent_1 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_1[0][0], sensor_coordinates_array_1[0][1]), softwareImageID_1, measurement_limit_1);
		temp_sens_sent_1.setSensorState(sens_state_1);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_1);
		
		SensorImpl temp_sens_received_1 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertNotEquals(temp_sens_sent_1.getSensorState(),				temp_sens_received_1.getSensorState());
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
												temp_sens_sent_1.getSensorState(),
												mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
												temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		temp_sens_received_1 = mockTCPclientTest.searchInClientSensorList(sensorID);
		
		assertTrue(receivedMessage instanceof ClientMessage_ACK);

		assertEquals(temp_sens_sent_1.getSensorState(),					temp_sens_received_1.getSensorState());
		
		assertFalse(clientManager_1.isClientManagerRunning());
	}	
	
	/***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that the messagesHandler() function updates watchdogs_scale_factor and measurements_limit of the TCPclient class instance regardless of
									the received sensor state in ServerMessage_SensorInfoUpdate. Verify also that Local_1h_Watchdog is enabled and its timeLeftBeforeExpiration
									is synchronized with Global_1h_Watchdog.timeLeftBeforeExpiration that was received in ServerMessage_SensorInfoUpdate.
	 * External variables TBV:		TCPclient.measurements_limit, TCPclient.watchdogs_scale_factor, Local_1h_Watchdog.millisecondsLeftUntilExpiration, Local_1h_Watchdog.isPaused
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		int sensorID = 1;
		float[][] sensor_coordinates_array_1 = {{11.0f, 14.0f}};
		String softwareImageID_1 = "Release X";
		int measurement_limit_1 = 5;
		SensorState sens_state_1 = SensorState.PRE_OPERATIONAL;
		double sens_watchdog_scale_factor_1 = 0.01;
		double received_Local_1h_watchdog = 1300;
		when(mockComputeEngine_Runnable.getLocal_1h_watchdog()).thenReturn(received_Local_1h_watchdog);
		
		SensorImpl temp_sens_sent_1 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_1[0][0], sensor_coordinates_array_1[0][1]), softwareImageID_1, measurement_limit_1);
		temp_sens_sent_1.setSensorState(sens_state_1);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_1);
		
		assertFalse(Local_1h_Watchdog.getInstance().getEnabled());
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
												temp_sens_sent_1.getSensorState(),
												mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
												temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertEquals(sens_watchdog_scale_factor_1, 			TCPclient.watchdogs_scale_factor, 0.01);
		assertEquals(measurement_limit_1, 					TCPclient.measurements_limit, 0.01);
		assertEquals(received_Local_1h_watchdog, 			Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.5);
		assertTrue(Local_1h_Watchdog.getInstance().getEnabled());
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensorID, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
	}	
	
	/***********************************************************************************************************
	 * Test Name: 					test_run_5
	 * Description: 				Verify that the messagesHandler() function sets the is ClientManagerRunning flag to false to close the TCP connection
									if the sensor state is set to SensorState.OPERATIONAL and the received Global_1h_Watchdog.timeLeftBeforeExpiration is higher than 
									Local_1h_Watchdog._1h_WatchdogExpiration divided by 4.
     * Internal variables TBV:		isClientManagerRunning
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		int sensorID = 1;
		float[][] sensor_coordinates_array_1 = {{11.0f, 14.0f}};
		String softwareImageID_1 = "Release X";
		int measurement_limit_1 = 5;
		SensorState sens_state_1 = SensorState.OPERATIONAL;
		double sens_watchdog_scale_factor_1 = 0.1;
		double received_Local_1h_watchdog = Local_1h_Watchdog.getInstance().getExpiration() * 0.25;
		when(mockComputeEngine_Runnable.getLocal_1h_watchdog()).thenReturn(received_Local_1h_watchdog);
		
		SensorImpl temp_sens_sent_1 = new SensorImpl(sensorID, new Point2D.Float(sensor_coordinates_array_1[0][0], sensor_coordinates_array_1[0][1]), softwareImageID_1, measurement_limit_1);
		temp_sens_sent_1.setSensorState(sens_state_1);
		temp_sens_sent_1.setSensor_watchdog_scale_factor(sens_watchdog_scale_factor_1);
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoUpdate(temp_sens_sent_1.getSensorID(), temp_sens_sent_1.getCoordinates(), temp_sens_sent_1.getSoftwareImageID(), 
												temp_sens_sent_1.getSensorState(),
												mockComputeEngine_Runnable.getLocal_1h_watchdog(), mockComputeEngine_Runnable.getLocal_24h_watchdog(),
												temp_sens_sent_1.getLocal_watchdog_scale_factor(), temp_sens_sent_1.getSensor_m_history_array_size()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertFalse(clientManager_1.isClientManagerRunning());
		Thread.sleep(20);
	}	

	@SuppressWarnings("static-access")
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+MessagesHandlerTest_ServerMessage_SensorInfoUpdate.testID+" teardown section:");
	   
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
