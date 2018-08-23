package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
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

import messages.Message_Interface;
import messages.SensorState;
import sensor.SensorImpl;
import tcpClient.ClientManager;
import tcpClient.TCPclient;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class Update_watchgods_after_TCP_connectionTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	int sensor_ID_2 = 2;
	int sensor_ID_3 = 3;
	int sensor_ID_4 = 4;
	int sensor_ID_5 = 5;
	int number_of_sensors = 5;
	int meas_limit = 5;
	SensorImpl sensor = null;
	final String serverHostName = "localhost";
	double global_watchdog_scale_factor = 0.01;
	ComputeEngine_Runnable comp_engine_1 = null;
	
	protected float[][] sensor_coordinates_array = { {1.0f, 1.0f}, {2.0f, 1.0f}, {1.5f, 2.0f}, {2.5f, 0.5f}, {3.0f, 3.5f}};
	protected String softwareImageID = "Release 1";
	
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
	
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
	
	// to mock TCPclient class instance
	TCPclient mockTCPclient = null;
		
	// to mock server threads
	Thread mockServerThread = null;
	
	String[] testPurpose = { "Verify that once the update_watchgods_after_TCP_connection() function is called if neither input_1hWatchog_timestamp_table nor input_24hWatchog_timestamp_table\r\n" + 
							 "contain only true values, neither Global_1h_Watchdog nor Global_24h_Watchdog are kicked. \r\n" + 
							 "Verify also that 1hWatchog_timestamp_table and 24hWatchog_timestamp_table are not updated",
							 "Verify that once the update_watchgods_after_TCP_connection() function is called if input_1hWatchog_timestamp_table contains only true values,\r\n" + 
							 "whereas input_24hWatchog_timestamp_table does not contain only true values, Global_1h_Watchdog is kicked, but Global_24h_Watchdog is not kicked\r\n" + 
							 "Verify also that 1hWatchog_timestamp_table is updated by setting all indexes to false, but 24hWatchog_timestamp_table is not updated",
							 "Verify that once the update_watchgods_after_TCP_connection() function is called if both input_1hWatchog_timestamp_table and input_24hWatchog_timestamp_table\r\n" + 
							 "contain only true values, both Global_1h_Watchdog and Global_24h_Watchdog are kicked\r\n" + 
							 "Verify also that both 1hWatchog_timestamp_table and 24hWatchog_timestamp_table are updated by setting all indexes to false",
							 "Verify that once the update_watchgods_after_TCP_connection() function is called if both input_1hWatchog_timestamp_table and input_24hWatchog_timestamp_table\r\n" + 
							 "contain only true values, all Measurements Datas files are deleted for a sensor that have called the function",
							 "Verify that once the update_watchgods_after_TCP_connection() function is called if neither input_1hWatchog_timestamp_table nor input_24hWatchog_timestamp_table contain only true values, Computing_time that measures duration of the TCP connection is not updated",
	 						 "Verify that once the update_watchgods_after_TCP_connection() function is called if both input_1hWatchog_timestamp_table and input_24hWatchog_timestamp_table contain only true values, Computing_time that measures duration of the TCP connection is updated"};

	static int testID = 1;
	
	public static void incrementTestID() {
		Update_watchgods_after_TCP_connectionTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException, ClassNotFoundException {
		
		// mocked objects 
		mockTCPserverTest = mock(TCPserver.class);
		mock_CER_ClientSocket = mock(Socket.class);
		mockClientManager = mock(ClientManager.class);
		mockTCPclient = mock(TCPclient.class);
		
		// create a real Server Socket for the TCPserver mock to enable the Client Socket to set up the TCP connection
		tempServerSocket_1 = new ServerSocket();
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		
		TCPserver.getInstance(port_1, number_of_sensors, meas_limit, TCPserver.getWatchdogs_scale_factor());
		TCPserver.processing_engine = new ComputeEngine_Processing();
		TCPserver.processing_engine.deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());
		
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
		
		System.out.println("\t\tTest Run "+Update_watchgods_after_TCP_connectionTest.testID+" Purpose:");
		System.out.println(testPurpose[(Update_watchgods_after_TCP_connectionTest.testID-1)]);
		System.out.println("\t\tTest Run "+Update_watchgods_after_TCP_connectionTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the update_watchgods_after_TCP_connection() function is called if neither input_1hWatchog_timestamp_table nor input_24hWatchog_timestamp_table
	 							contain only true values, neither Global_1h_Watchdog nor Global_24h_Watchdog are kicked. 
	 							Verify also that 1hWatchog_timestamp_table and 24hWatchog_timestamp_table are not updated
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration, TCPserver._1hWatchog_timestamp_table
	 							TCPserver._24hWatchog_timestamp_table
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		double input_watchdog = 100 * global_watchdog_scale_factor;
		double expected_1h_Global_watchdog_readout = input_watchdog;
		double expected_24h_Global_watchdog_readout = input_watchdog;
		TCPserver.setComputing_time(input_watchdog * 1.5);
		
		boolean[] input_1hWatchog_timestamp_table = {true, false, false, false, false};
		boolean[] input_24hWatchog_timestamp_table = {true, false, false, false, false};
		boolean[] expected_1hWatchog_timestamp_table = {true, false, false, false, false};
		boolean[] expected_24hWatchog_timestamp_table = {true, false, false, false, false};
		
		for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
    		sensor.setSensorState(SensorState.OPERATIONAL);
    		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
    		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
    		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
			TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
			TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
    	}
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		comp_engine_1.update_watchgods_after_TCP_connection(input_1hWatchog_timestamp_table, input_24hWatchog_timestamp_table, sensor);
		
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		
		
		for (int i = 0; i < input_1hWatchog_timestamp_table.length; i++) {
			assertEquals(expected_1hWatchog_timestamp_table[i],		TCPserver.get_1hWatchog_timestamp_table().get()[i]);
			assertEquals(expected_24hWatchog_timestamp_table[i],	TCPserver.get_24hWatchog_timestamp_table().get()[i]);
		}
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the update_watchgods_after_TCP_connection() function is called if input_1hWatchog_timestamp_table contains only true values,
								whereas input_24hWatchog_timestamp_table does not contain only true values, Global_1h_Watchdog is kicked, but Global_24h_Watchdog is not kicked
	 							Verify also that 1hWatchog_timestamp_table is updated by setting all indexes to false, but 24hWatchog_timestamp_table is not updated
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration, TCPserver._1hWatchog_timestamp_table
	 							TCPserver._24hWatchog_timestamp_table
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		double input_watchdog = 100 * global_watchdog_scale_factor;
		TCPserver.setComputing_time(input_watchdog * 1.5);
		double expected_24h_Global_watchdog_readout = input_watchdog;
		double expected_1h_Global_watchdog_readout = Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor + input_watchdog * 0.75;
		
		boolean[] input_1hWatchog_timestamp_table = {true, true, true, true, true};
		boolean[] input_24hWatchog_timestamp_table = {true, false, false, false, false};
		boolean[] expected_1hWatchog_timestamp_table = {false, false, false, false, false};
		boolean[] expected_24hWatchog_timestamp_table = {true, false, false, false, false};
		
		for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
    		sensor.setSensorState(SensorState.OPERATIONAL);
    		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
    		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
    		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
			TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
			TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
    	}
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		comp_engine_1.update_watchgods_after_TCP_connection(input_1hWatchog_timestamp_table, input_24hWatchog_timestamp_table, sensor);
		
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		
		for (int i = 0; i < input_1hWatchog_timestamp_table.length; i++) {
			assertEquals(expected_1hWatchog_timestamp_table[i],		TCPserver.get_1hWatchog_timestamp_table().get()[i]);
			assertEquals(expected_24hWatchog_timestamp_table[i],	TCPserver.get_24hWatchog_timestamp_table().get()[i]);
		}
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the update_watchgods_after_TCP_connection() function is called if both input_1hWatchog_timestamp_table and input_24hWatchog_timestamp_table
	  							contain only true values, both Global_1h_Watchdog and Global_24h_Watchdog are kicked
	 							Verify also that both 1hWatchog_timestamp_table and 24hWatchog_timestamp_table are updated by setting all indexes to false
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration, TCPserver._1hWatchog_timestamp_table
	 							TCPserver._24hWatchog_timestamp_table
     * Exceptions thrown: 		IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException, ClassNotFoundException {		
		
		double input_watchdog = 600 * global_watchdog_scale_factor;
		double expected_1h_Global_watchdog_readout = Global_1h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor + input_watchdog * 0.5;
		double expected_24h_Global_watchdog_readout = Global_24h_Watchdog.getInstance().getExpiration() * global_watchdog_scale_factor * TCPserver.getMeasurements_limit();
		TCPserver.setComputing_time(input_watchdog * 1.5);
		
		boolean[] input_1hWatchog_timestamp_table = {true, true, true, true, true};
		boolean[] input_24hWatchog_timestamp_table = {true, true, true, true, true};		
		boolean[] expected_1hWatchog_timestamp_table = {false, false, false, false, false};
		boolean[] expected_24hWatchog_timestamp_table = {false, false, false, false, false};
		
		for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
    		sensor.setSensorState(SensorState.OPERATIONAL);
    		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
    		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
    		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
			TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
			TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
    	}
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setServer_measurements_limit(TCPserver.getMeasurements_limit());
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		for (int i = 0; i < TCPserver.getMeasurements_limit(); i++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
			TCPserver.processing_engine.saveMeasurementDataInfo(new SensorImpl(sensor.getSensorID()), sensor.readLastMeasurementData());
			Thread.sleep(comp_engine_1.get_delays(Delays.MEDIUM, comp_engine_1.getDelays_array()));
		}
		
		comp_engine_1.update_watchgods_after_TCP_connection(input_1hWatchog_timestamp_table, input_24hWatchog_timestamp_table, sensor);
		
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 2.0);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 2.0);
		
		Thread.sleep(comp_engine_1.get_delays(Delays.HIGHEST, comp_engine_1.getDelays_array()));
		
		for (int i = 0; i < input_1hWatchog_timestamp_table.length; i++) {
			assertEquals(expected_1hWatchog_timestamp_table[i],		TCPserver.get_1hWatchog_timestamp_table().get()[i]);
			assertEquals(expected_24hWatchog_timestamp_table[i],	TCPserver.get_24hWatchog_timestamp_table().get()[i]);
		}
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the update_watchgods_after_TCP_connection() function is called if both input_1hWatchog_timestamp_table and input_24hWatchog_timestamp_table
	  							contain only true values, all Measurements Datas files are deleted for a sensor that have called the function 
	 * External variables TBV: 	TCPserver.Sensors_PATH, TCPserver.processing_engine
     * Exceptions thrown: 		IOException, InterruptedException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException, ClassNotFoundException {		
		
		double input_watchdog = 100 * global_watchdog_scale_factor;
		TCPserver.setComputing_time(input_watchdog * 1.5);
		boolean[] input_1hWatchog_timestamp_table = {true, true, true, true, true};
		boolean[] input_24hWatchog_timestamp_table = {true, true, true, true, true};		
		
		for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
    		sensor.setSensorState(SensorState.OPERATIONAL);
    		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
    		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
    		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
			TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
			TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
    	}
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setServer_measurements_limit(TCPserver.getMeasurements_limit());
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		for (int i = 0; i < TCPserver.getMeasurements_limit(); i++) {
			double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
			int humidity = ThreadLocalRandom.current().nextInt(0, 101);
			int temperature = ThreadLocalRandom.current().nextInt(0, 30);
			int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
			sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
			TCPserver.processing_engine.saveMeasurementDataInfo(new SensorImpl(sensor.getSensorID()), sensor.readLastMeasurementData());
			Thread.sleep(comp_engine_1.get_delays(Delays.HIGHEST, comp_engine_1.getDelays_array()));
		}
		
		int expected_file_count_before = TCPserver.getMeasurements_limit();
		int actual_file_count_before = 0;
		int expected_file_count_after = 0;
		int actual_file_count_after = 0;
		
		File sensor_path = null;
		sensor_path = new java.io.File(TCPserver.getSensorsPath() + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "measurement_Datas");
		for (@SuppressWarnings("unused") File file :  sensor_path.listFiles()) {
			actual_file_count_before += 1;
		}
		
		assertEquals(expected_file_count_before,		actual_file_count_before);
		
		comp_engine_1.update_watchgods_after_TCP_connection(input_1hWatchog_timestamp_table, input_24hWatchog_timestamp_table, sensor);
		
		expected_file_count_after = 0;
		for (@SuppressWarnings("unused") File file :  sensor_path.listFiles()) {
			actual_file_count_after += 1;
		}
		
		assertEquals(expected_file_count_after,		actual_file_count_after);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that once the update_watchgods_after_TCP_connection() function is called if neither input_1hWatchog_timestamp_table nor input_24hWatchog_timestamp_table
	 							contain only true values, Computing_time that measures duration of the TCP connection is not updated
	 * External variables TBV: 	TCPserver.computing_time
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException, InterruptedException {
		
		double input_watchdog = 100 * global_watchdog_scale_factor;
		TCPserver.setComputing_time(input_watchdog * 1.5);
		
		boolean[] input_1hWatchog_timestamp_table = {true, false, false, false, false};
		boolean[] input_24hWatchog_timestamp_table = {true, false, false, false, false};
		
		for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
    		sensor.setSensorState(SensorState.OPERATIONAL);
    		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
    		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
    		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
			TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
			TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
    	}
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		comp_engine_1.update_watchgods_after_TCP_connection(input_1hWatchog_timestamp_table, input_24hWatchog_timestamp_table, sensor);
		
		double expected_couputed_time = input_watchdog * 1.5;
		
		assertEquals(expected_couputed_time, 		TCPserver.getComputing_time(), 0.1);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that once the update_watchgods_after_TCP_connection() function is called if both input_1hWatchog_timestamp_table and input_24hWatchog_timestamp_table
	  							contain only true values, Computing_time that measures duration of the TCP connection is updated
	 * External variables TBV: 	TCPserver.computing_time
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_6() throws IOException, InterruptedException {
		
		double input_watchdog = 100 * global_watchdog_scale_factor;
		TCPserver.setComputing_time(input_watchdog * 1.5);
		
		boolean[] input_1hWatchog_timestamp_table = {true, true, true, true, true};
		boolean[] input_24hWatchog_timestamp_table = {true, false, false, false, false};
		
		for (int i = 1; i <= sensor_coordinates_array.length; i++) {
    		sensor = new SensorImpl(i, new Point2D.Float(sensor_coordinates_array[i-1][0], sensor_coordinates_array[i-1][1]), softwareImageID, TCPserver.getMeasurements_limit());
    		sensor.setSensorState(SensorState.OPERATIONAL);
    		sensor.setSensor_watchdog_scale_factor(global_watchdog_scale_factor);
    		TCPserver.Server_Sensors_LIST = TCPserver.processing_engine.updateServerSensorList(sensor);
    		TCPserver.processing_engine.saveSensorInfo(sensor, "sensorINITIALIZATION");
			TCPserver.set_1hWatchog_Timestamp_tableID_value(input_1hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
			TCPserver.set_24hWatchog_Timestamp_tableID_value(input_24hWatchog_timestamp_table[i-1], sensor.getSensorID()-1);
    	}
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double time_left_before_1h_watchdog_kicked = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		
		comp_engine_1.update_watchgods_after_TCP_connection(input_1hWatchog_timestamp_table, input_24hWatchog_timestamp_table, sensor);
		
		double expected_couputed_time = input_watchdog * 1.5 - time_left_before_1h_watchdog_kicked;
		
		assertEquals(expected_couputed_time, 		TCPserver.getComputing_time(), 0.1);
	}
	
	@After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+Update_watchgods_after_TCP_connectionTest.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
		   
}
