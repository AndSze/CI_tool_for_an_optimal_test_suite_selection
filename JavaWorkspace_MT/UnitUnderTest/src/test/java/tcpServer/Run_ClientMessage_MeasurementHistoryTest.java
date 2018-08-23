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
import java.util.concurrent.ThreadLocalRandom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import messages.ClientMessage_MeasurementData;
import messages.ClientMessage_MeasurementHistory;
import messages.Message_Interface;
import messages.SensorState;
import messages.ServerMessage_ACK;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.MeasurementData;
import sensor.SensorImpl;
import tcpClient.ClientManager;
import tcpClient.TCPclient;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class Run_ClientMessage_MeasurementHistoryTest {

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
	
	String[] testPurpose = { "Verify that the run() function responds to ClientMessage_MeasurementHistory with ServerMessage_SensorInfoUpdate",
							 "Verify that the run() updates index that represents the sensor in _24hWatchog_timestamp_table to true and kicks Local_24h_Watchdog if ClientMessage_MeasurementHistory was received",
							 "Verify that once the run() function receives ClientMessage_MeasurementHistory, this measurement history is serialized and saved in Sensors_PATH directory on the disc",
							 "Verify that once the run() function receives ClientMessage_MeasurementHistory, after saving the measurement history, the sesnor is reset and its instance in Server_Sensors_LIST is updated"};

	static int testID = 1;
	
	public static void incrementTestID() {
		Run_ClientMessage_MeasurementHistoryTest.testID += 1;
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
		
		System.out.println("\t\tTest Run "+Run_ClientMessage_MeasurementHistoryTest.testID+" Purpose:");
		System.out.println(testPurpose[(Run_ClientMessage_MeasurementHistoryTest.testID-1)]);
		System.out.println("\t\tTest Run "+Run_ClientMessage_MeasurementHistoryTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that the run() function responds to ClientMessage_MeasurementHistory with ServerMessage_SensorInfoUpdate
	 * External variables TBV:	 	ClientMessage_MeasurementHistory, ServerMessage_SensorInfoUpdate
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.OPERATIONAL);
		
		for (int measurements = 0; measurements < TCPserver.getMeasurements_limit() - 1; measurements++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			temp_sensor_client.addMeasurement(pm25, pm10, humidity, temperature, pressure);
		}
		
		MeasurementData[] client_mes_hist = temp_sensor_client.readMeasurementHistory();
		
		mockClientManager.sendMessage(new ClientMessage_MeasurementHistory(sensor_ID_1, client_mes_hist), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoUpdate);
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that the run() updates index that represents the sensor in _24hWatchog_timestamp_table to true
	  								and kicks Local_24h_Watchdog if ClientMessage_MeasurementHistory was received
	 * Internal variables TBV:		local_24h_watchdog
	 * External variables TBV:	 	TCPserver._24hWatchog_timestamp_table
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.OPERATIONAL);
		
		for (int measurements = 0; measurements < TCPserver.getMeasurements_limit() - 1; measurements++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			temp_sensor_client.addMeasurement(pm25, pm10, humidity, temperature, pressure);
		}
		
		MeasurementData[] client_mes_hist = temp_sensor_client.readMeasurementHistory();
		
		double local_24h_watchdog_before_meas_data = Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		double local_24h_watchdog_after_meas_data = Global_24h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor * TCPserver.getMeasurements_limit();
		
		assertEquals(local_24h_watchdog_before_meas_data,  			comp_engine_1.getLocal_24h_watchdog(), 0.1);
		assertFalse(TCPserver.isIDTrue(TCPserver.get_24hWatchog_timestamp_table().get(), sensor_ID_1));
		
		mockClientManager.sendMessage(new ClientMessage_MeasurementHistory(sensor_ID_1, client_mes_hist), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		assertEquals(local_24h_watchdog_after_meas_data,  			comp_engine_1.getLocal_24h_watchdog(), 0.1);
		assertTrue(TCPserver.isIDTrue(TCPserver.get_24hWatchog_timestamp_table().get(), sensor_ID_1));
		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that once the run() function receives ClientMessage_MeasurementHistory, this measurement history is serialized and saved in Sensors_PATH directory on the disc
	 * External variables TBV:	 	TCPserver.Sensors_PATH, SensorImpl.sensor_m_history
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.OPERATIONAL);
		
		for (int measurements = 0; measurements < TCPserver.getMeasurements_limit(); measurements++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			temp_sensor_client.addMeasurement(pm25, pm10, humidity, temperature, pressure);
			
			MeasurementData client_mes_data = temp_sensor_client.readLastMeasurementData();
			
			Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
			
			mockClientManager.sendMessage(new ClientMessage_MeasurementData(sensor_ID_1, client_mes_data), mockClientManager.getOutputStream());
			Thread.sleep(50);
			
			assertTrue(receivedMessage instanceof ServerMessage_ACK);
			Thread.sleep(50);
		}
		
		MeasurementData[] client_mes_hist = temp_sensor_client.readMeasurementHistory();
		
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(120 * global_watchdog_scale_factor);
		
		mockClientManager.sendMessage(new ClientMessage_MeasurementHistory(sensor_ID_1, client_mes_hist), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		File sensor_path = null;
		MeasurementData[] server_mes_hist = null;
		sensor_path = new java.io.File(TCPserver.getSensorsPath() + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "measurement_Histories");
		for ( File file :  sensor_path.listFiles()) {
			server_mes_hist = (MeasurementData[]) TCPserver.processing_engine.deserialize(file.getAbsolutePath(), MeasurementData[].class);
		}
		
		for (int measurements = 0; measurements < TCPserver.getMeasurements_limit(); measurements++) {
			assertEquals(client_mes_hist[measurements].getPm10(), 				server_mes_hist[measurements].getPm10(), 0.01);
			assertEquals(client_mes_hist[measurements].getPm25(), 				server_mes_hist[measurements].getPm25(), 0.01);
			assertEquals(client_mes_hist[measurements].getPressure(), 			server_mes_hist[measurements].getPressure());
			assertEquals(client_mes_hist[measurements].getHumidity(),			server_mes_hist[measurements].getHumidity());
			assertEquals(client_mes_hist[measurements].getTemperature(), 		server_mes_hist[measurements].getTemperature());
		}
		
		Thread.sleep(50);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that once the run() function receives ClientMessage_MeasurementHistory, after saving the measurement history,
	  								the sensor is reset and its instance in Server_Sensors_LIST is updated
	 * External variables TBV:	 	TCPserver.Sensors_PATH, SensorImpl.sensor_m_history, SensorImpl.sensorState, SensorImpl.numberOfMeasurements
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
		
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test run() is called in this thread
		testThread_server.start();
		Thread.sleep(20);

		SensorImpl temp_sensor_client = new SensorImpl(sensor_ID_1, new Point2D.Float(sensor_coordinates_array[sensor_ID_1-1][0], sensor_coordinates_array[sensor_ID_1-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
		temp_sensor_client.setSensorState(SensorState.OPERATIONAL);
		
		for (int measurements = 0; measurements < TCPserver.getMeasurements_limit(); measurements++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			temp_sensor_client.addMeasurement(pm25, pm10, humidity, temperature, pressure);
			
			MeasurementData client_mes_data = temp_sensor_client.readLastMeasurementData();
			
			Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
			
			mockClientManager.sendMessage(new ClientMessage_MeasurementData(sensor_ID_1, client_mes_data), mockClientManager.getOutputStream());
			Thread.sleep(50);
			
			assertTrue(receivedMessage instanceof ServerMessage_ACK);
			Thread.sleep(50);
		}
		
		MeasurementData[] client_mes_hist = temp_sensor_client.readMeasurementHistory();
		
		SensorImpl server_sensor_before_meas_hist = null;
		SensorImpl server_sensor_after_meas_hist = null;
		
		server_sensor_before_meas_hist = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		int expected_no_of_meas_before_reset = 24;
		
		MeasurementData[] server_mes_hist = server_sensor_before_meas_hist.readMeasurementHistory();
		
		assertEquals(expected_no_of_meas_before_reset, 		server_sensor_before_meas_hist.getNumberOfMeasurements());
		
		for (int measurements = 0; measurements < TCPserver.getMeasurements_limit(); measurements++) {
			assertEquals(client_mes_hist[measurements].getPm10(), 				server_mes_hist[measurements].getPm10(), 0.01);
			assertEquals(client_mes_hist[measurements].getPm25(), 				server_mes_hist[measurements].getPm25(), 0.01);
			assertEquals(client_mes_hist[measurements].getPressure(), 			server_mes_hist[measurements].getPressure());
			assertEquals(client_mes_hist[measurements].getHumidity(),			server_mes_hist[measurements].getHumidity());
			assertEquals(client_mes_hist[measurements].getTemperature(), 		server_mes_hist[measurements].getTemperature());
		}
		
		Global_24h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(80 * global_watchdog_scale_factor);
				
		mockClientManager.sendMessage(new ClientMessage_MeasurementHistory(sensor_ID_1, client_mes_hist), mockClientManager.getOutputStream());
		Thread.sleep(50);
		
		server_sensor_after_meas_hist = TCPserver.getProcessing_engine().searchInServerSensorList(sensor_ID_1);
		int expected_no_of_meas_after_reset = 0;
		SensorState expected_sensor_state_after_reset = SensorState.PRE_OPERATIONAL;
		
		assertEquals(expected_no_of_meas_after_reset, 		server_sensor_after_meas_hist.getNumberOfMeasurements());
		assertEquals(expected_sensor_state_after_reset, 	server_sensor_after_meas_hist.getSensorState());
		
		Thread.sleep(50);
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+Run_ClientMessage_MeasurementHistoryTest.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}

}
