package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.awt.geom.Point2D;
import java.io.EOFException;
import java.io.File;
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
import messages.ClientMessage_SensorInfo;
import messages.Message_Interface;
import messages.SensorState;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.SensorImpl;
import tcpClient.ClientManager;
import tcpClient.TCPclient;

public class Run_ClientMessage_SensorInfoTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	int number_of_sensors = 1;
	int measurements_limit = 24;
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
	
	String[] testPurpose = { "Verify that the run() function responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate in case of configuration match between sensors on the server and client side",
			      	 	 	 "Verify that the run() function responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate in case of configuration mismatch between sensors on the server and client side",
			      	 	 	 "Verify that the run() function updates SensorState to OPERATIONAL and saves the current sensor info on PC disc with the \"_gotoOPERATIONALafterRESET \" extension if \n ClientMessage_SensorInfo is received when sensor was in PRE_OPERATIONAL SensorState",
			      	 	 	 "Verify that the run() function updates SensorState to OPERATIONAL and saves the current sensor info on PC disc with the \"gotoOPERATIONALafterCONFIGURATION \" extension if \n ClientMessage_SensorInfo is received when sensor was in MAINETANCE SensorState",
			      	 	 	 "Verify that the run() function updates SensorState to OPERATIONAL and saves the current sensor info on PC disc with the \"stayinOPERATIONAL \" extension if \n ClientMessage_SensorInfo is received when sensor was in OPERATIONAL SensorState",
			      	 	 	 "Verify that the run() function updates SensorState to MAINETANCE and saves the current sensor info on PC disc with the \"gotoMAINTENANCEafterINITIALIZATION \" extension if \n ClientMessage_SensorInfo is received when there was configuration mismatch between sensors on the server and client side"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		Run_ClientMessage_SensorInfoTest.testID += 1;
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
		
		TCPserver.getInstance(port_1, number_of_sensors, measurements_limit, TCPserver.getWatchdogs_scale_factor());
		TCPserver.processing_engine = new ComputeEngine_Processing();
		TCPserver.processing_engine.deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());
		
		sensor = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		sensor.setSensorState(SensorState.OPERATIONAL);
		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[sensor_ID_1-1], sensor.getSensorID()-1);
		TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[sensor_ID_1-1], sensor.getSensorID()-1);
		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
		
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
		
		System.out.println("\t\tTest Run "+Run_ClientMessage_SensorInfoTest.testID+" Purpose:");
		System.out.println(testPurpose[(Run_ClientMessage_SensorInfoTest.testID-1)]);
		System.out.println("\t\tTest Run "+Run_ClientMessage_SensorInfoTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that the run() function responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate
	  								in case of configuration match between sensors on the server and client side
	 * External variables TBV:	 	ClientMessage_SensorInfo, ServerMessage_SensorInfoUpdate, SensorImpl.coordinates, SensorImpl.softwareImageID, 
	 								SensorImpl.sensorState, SensorImpl.sensor_watchdog_scale_factor
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException, ClassNotFoundException {
		
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
		
		// create a sensor on the client side that has the same configuration
		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.PRE_OPERATIONAL);
		temp_sensor_client.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		
		SensorImpl server_sensor_before_sensor_info = null;
		
		sensor.setSensorState(SensorState.PRE_OPERATIONAL);
		TCPserver.getProcessing_engine().updateServerSensorList(sensor);
		
		server_sensor_before_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		
		assertEquals(temp_sensor_client.getSensorState(),					server_sensor_before_sensor_info.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					server_sensor_before_sensor_info.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				server_sensor_before_sensor_info.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	server_sensor_before_sensor_info.getLocal_watchdog_scale_factor(), 0.01);

		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_sensor_client), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoUpdate);
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that the run() function responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate
	  								in case of configuration mismatch between sensors on the server and client side
	 * External variables TBV:	 	ClientMessage_SensorInfo, ServerMessage_SensorInfoUpdate, SensorImpl.coordinates, SensorImpl.softwareImageID, 
	 								SensorImpl.sensorState, SensorImpl.sensor_watchdog_scale_factor
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException, ClassNotFoundException {
		
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
		
		// create a sensor on the client side that has different configuration (PRE_OPERATIONAL state)
		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.PRE_OPERATIONAL);
		temp_sensor_client.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		
		SensorImpl server_sensor_before_sensor_info = null;
		
		server_sensor_before_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		
		assertNotEquals(temp_sensor_client.getSensorState(),				server_sensor_before_sensor_info.getSensorState());

		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_sensor_client), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoUpdate);
		Thread.sleep(50);
	}
		
    /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that the run() function updates SensorState to OPERATIONAL and saves the current sensor info on PC disc with the \"_gotoOPERATIONALafterRESET \" extension
	 								if ClientMessage_SensorInfo is received when sensor was in PRE_OPERATIONAL SensorState
	 * External variables TBV:	 	TCPserver.Sensors_PATH, SensorImpl.sensorState
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException, ClassNotFoundException {
		
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

		// create a sensor on the client side that has the same configuration (test condition for SensorState.PRE_OPERATIONAL)
		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.PRE_OPERATIONAL);
		temp_sensor_client.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		
		SensorImpl server_sensor_before_sensor_info = null;
		SensorImpl server_sensor_after_sensor_info = null;
		
		sensor.setSensorState(SensorState.PRE_OPERATIONAL);
		TCPserver.getProcessing_engine().updateServerSensorList(sensor);
		
		server_sensor_before_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		
		assertEquals(temp_sensor_client.getSensorState(),					server_sensor_before_sensor_info.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					server_sensor_before_sensor_info.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				server_sensor_before_sensor_info.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	server_sensor_before_sensor_info.getLocal_watchdog_scale_factor(), 0.01);
		
		// prior to sending ClientMessage_SensorInfo, all file are removed from the Sensors_PATH directory (e.g. _sensorINITIALIZATION.sensor_info)
		TCPserver.processing_engine.deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());
		
		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_sensor_client), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		server_sensor_after_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		SensorState exptected_sensor_state_after_sensor_info = SensorState.OPERATIONAL;

		assertEquals(exptected_sensor_state_after_sensor_info,				server_sensor_after_sensor_info.getSensorState());
		
		String expected_extension_name = "_gotoOPERATIONALafterRESET.sensor_info";
		
		File sensor_path = null;
		File temp_file = null;
		SensorImpl sensor_info_saved = null;
		sensor_path = new java.io.File(TCPserver.getSensorsPath() + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "sensor_Infos");
		for ( File file :  sensor_path.listFiles()) {
			sensor_info_saved = (SensorImpl) TCPserver.processing_engine.deserialize(file.getAbsolutePath(), SensorImpl.class);
			temp_file = file;
		}
		
		assertEquals(expected_extension_name, 	temp_file.getName().substring(temp_file.getName().length() - expected_extension_name.length()));
		
		assertEquals(exptected_sensor_state_after_sensor_info,				sensor_info_saved.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					sensor_info_saved.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				sensor_info_saved.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	sensor_info_saved.getLocal_watchdog_scale_factor(), 0.01);
		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that the run() function updates SensorState to OPERATIONAL and saves the current sensor info on PC disc with the \"gotoOPERATIONALafterCONFIGURATION \" extension 
	 								if ClientMessage_SensorInfo is received when sensor was in MAINETANCE SensorState
	 * External variables TBV:	 	TCPserver.Sensors_PATH, SensorImpl.sensorState
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException, ClassNotFoundException {
		
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

		// create a sensor on the client side that has the same configuration (test condition for SensorState.MAINTENANCE)
		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.MAINTENANCE);
		temp_sensor_client.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		
		SensorImpl server_sensor_before_sensor_info = null;
		SensorImpl server_sensor_after_sensor_info = null;
		
		sensor.setSensorState(SensorState.MAINTENANCE);
		TCPserver.getProcessing_engine().updateServerSensorList(sensor);
		
		server_sensor_before_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		
		assertEquals(temp_sensor_client.getSensorState(),					server_sensor_before_sensor_info.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					server_sensor_before_sensor_info.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				server_sensor_before_sensor_info.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	server_sensor_before_sensor_info.getLocal_watchdog_scale_factor(), 0.01);
		
		// prior to sending ClientMessage_SensorInfo, all file are removed from the Sensors_PATH directory (e.g. _sensorINITIALIZATION.sensor_info)
		TCPserver.processing_engine.deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());
		
		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_sensor_client), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		server_sensor_after_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		SensorState exptected_sensor_state_after_sensor_info = SensorState.OPERATIONAL;

		assertEquals(exptected_sensor_state_after_sensor_info,				server_sensor_after_sensor_info.getSensorState());
		
		String expected_extension_name = "gotoOPERATIONALafterCONFIGURATION.sensor_info";
		
		File sensor_path = null;
		File temp_file = null;
		SensorImpl sensor_info_saved = null;
		sensor_path = new java.io.File(TCPserver.getSensorsPath() + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "sensor_Infos");
		for ( File file :  sensor_path.listFiles()) {
			sensor_info_saved = (SensorImpl) TCPserver.processing_engine.deserialize(file.getAbsolutePath(), SensorImpl.class);
			temp_file = file;
		}
		
		assertEquals(expected_extension_name, 	temp_file.getName().substring(temp_file.getName().length() - expected_extension_name.length()));
		
		assertEquals(exptected_sensor_state_after_sensor_info,				sensor_info_saved.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					sensor_info_saved.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				sensor_info_saved.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	sensor_info_saved.getLocal_watchdog_scale_factor(), 0.01);
		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_5
	 * Description: 				Verify that the run() function updates SensorState to OPERATIONAL and saves the current sensor info on PC disc with the \"stayinOPERATIONAL \" extension
	 								if ClientMessage_SensorInfo is received when sensor was in OPERATIONAL SensorState
	 * External variables TBV:	 	TCPserver.Sensors_PATH, SensorImpl.sensorState
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException, InterruptedException, ClassNotFoundException {
		
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

		// create a sensor on the client side that has the same configuration (test condition for SensorState.OPERATIONAL)
		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.OPERATIONAL);
		temp_sensor_client.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		
		SensorImpl server_sensor_before_sensor_info = null;
		SensorImpl server_sensor_after_sensor_info = null;
		
		server_sensor_before_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		
		assertEquals(temp_sensor_client.getSensorState(),					server_sensor_before_sensor_info.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					server_sensor_before_sensor_info.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				server_sensor_before_sensor_info.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	server_sensor_before_sensor_info.getLocal_watchdog_scale_factor(), 0.01);
		
		// prior to sending ClientMessage_SensorInfo, all file are removed from the Sensors_PATH directory (e.g. _sensorINITIALIZATION.sensor_info)
		TCPserver.processing_engine.deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());
		
		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_sensor_client), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		server_sensor_after_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		SensorState exptected_sensor_state_after_sensor_info = SensorState.OPERATIONAL;

		assertEquals(exptected_sensor_state_after_sensor_info,				server_sensor_after_sensor_info.getSensorState());
		
		String expected_extension_name = "stayinOPERATIONAL.sensor_info";
		
		File sensor_path = null;
		File temp_file = null;
		SensorImpl sensor_info_saved = null;
		sensor_path = new java.io.File(TCPserver.getSensorsPath() + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "sensor_Infos");
		for ( File file :  sensor_path.listFiles()) {
			sensor_info_saved = (SensorImpl) TCPserver.processing_engine.deserialize(file.getAbsolutePath(), SensorImpl.class);
			temp_file = file;
		}
		
		assertEquals(expected_extension_name, 	temp_file.getName().substring(temp_file.getName().length() - expected_extension_name.length()));
		
		assertEquals(exptected_sensor_state_after_sensor_info,				sensor_info_saved.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					sensor_info_saved.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				sensor_info_saved.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	sensor_info_saved.getLocal_watchdog_scale_factor(), 0.01);
		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_6
	 * Description: 				"Verify that the run() function updates SensorState to MAINETANCE and saves the current sensor info on PC disc with the \"gotoMAINTENANCEafterINITIALIZATION \" extension if 
	 								ClientMessage_SensorInfo is received when there was configuration mismatch between sensors on the server and client side
	 * External variables TBV:	 	TCPserver.Sensors_PATH, SensorImpl.sensorState
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.readMessage(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_6() throws IOException, InterruptedException, ClassNotFoundException {
		
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

		// create a sensor on the client side that has different configuration (PRE_OPERATIONAL state)
		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.PRE_OPERATIONAL);
		temp_sensor_client.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
		
		SensorImpl server_sensor_before_sensor_info = null;
		SensorImpl server_sensor_after_sensor_info = null;
		
		server_sensor_before_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		
		assertNotEquals(temp_sensor_client.getSensorState(),				server_sensor_before_sensor_info.getSensorState());
		
		// prior to sending ClientMessage_SensorInfo, all file are removed from the Sensors_PATH directory (e.g. _sensorINITIALIZATION.sensor_info)
		TCPserver.processing_engine.deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());

		mockClientManager.sendMessage(new ClientMessage_SensorInfo(temp_sensor_client), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		server_sensor_after_sensor_info = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		SensorState exptected_sensor_state_after_sensor_info = SensorState.MAINTENANCE;

		assertEquals(exptected_sensor_state_after_sensor_info,				server_sensor_after_sensor_info.getSensorState());
		
		String expected_extension_name = "gotoMAINTENANCEafterINITIALIZATION.sensor_info";
		
		File sensor_path = null;
		File temp_file = null;
		SensorImpl sensor_info_saved = null;
		sensor_path = new java.io.File(TCPserver.getSensorsPath() + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "sensor_Infos");
		for ( File file :  sensor_path.listFiles()) {
			sensor_info_saved = (SensorImpl) TCPserver.processing_engine.deserialize(file.getAbsolutePath(), SensorImpl.class);
			temp_file = file;
		}
		
		assertEquals(expected_extension_name, 	temp_file.getName().substring(temp_file.getName().length() - expected_extension_name.length()));
		
		assertEquals(exptected_sensor_state_after_sensor_info,				sensor_info_saved.getSensorState());
		assertEquals(temp_sensor_client.getCoordinates(),					sensor_info_saved.getCoordinates());
		assertEquals(temp_sensor_client.getSoftwareImageID(),				sensor_info_saved.getSoftwareImageID());
		assertEquals(temp_sensor_client.getLocal_watchdog_scale_factor(),	sensor_info_saved.getLocal_watchdog_scale_factor(), 0.01);
		
		Thread.sleep(50);
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+Run_ClientMessage_SensorInfoTest.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}

}
