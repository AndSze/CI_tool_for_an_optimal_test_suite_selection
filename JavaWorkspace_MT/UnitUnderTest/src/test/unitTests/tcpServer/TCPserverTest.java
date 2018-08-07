package tcpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.BindException;
import java.net.SocketException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sensor.SensorImpl;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class TCPserverTest {
	
	TCPserver tcpserver_1 = null;
	int port_1 = 9879;
	Thread testThread_exception = null;
	
	String[] testPurpose = { "Verify that once the overloaded constructor of the TCPserver class is called, the TCPserver class instance is updated with new server socket that is bound to the port and has ReuseAddress set to TRUE",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the TCPserver class instance is updated with the serverRunning flag set to TRUE",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the 1h_Watchdog and 24h_Watchdog instances are enabled along with their threads",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the instance of the ComputeEngine_Processing class for processing serialized files is created",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the sensor instances are created in accordance with the TCPclass attributes and then they are saved in Server_Sensors_LIST",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the flags that indicate if the watchdogs have been kicked are set to FALSE in the 1hWatchog and 24hWatchdog timestamp tables",  
							 "Verify that once the overloaded constructor of the TCPserver class is called, the startServer() function is called as last step in the constructor that trigger communication via a TCP connection with sensors in a dedicated thread",
							 "Verify that once the overloaded constructor of the TCPserver class is called multiple times with the same port ID without closing the TCPclass instance created on this port, the BindException is thrown "};
	static int testID = 1;
	
	public static void incrementTestID() {
		TCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpserver_1 = new TCPserver(port_1);
		
		System.out.println("\t\tTest Run "+TCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(TCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+TCPserverTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 * 							the TCPserver class instance is being updated with new server socket that is bound to the port and has ReuseAddress set to TRUE
	 * Internal variables TBV: 	serverSocket
      * Exceptions thrown:		SocketException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws SocketException {

		assertNotEquals(null, tcpserver_1.getServerSocket());
		assertTrue(tcpserver_1.getServerSocket().isBound());
		assertTrue(tcpserver_1.getServerSocket().getReuseAddress());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 * 							the TCPserver class instance is being updated with the serverRunning flag set to TRUE
	 * Internal variables TBV: 	serverRunning
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {

		assertTrue(tcpserver_1.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is being called, the 1h_Watchdog and 24h_Watchdog instances are enabled along with their threads
	 * External variables TBV: 	Global_1h_Watchdog.isPaused, Global_24h_Watchdog.isPaused, Global_1h_Watchdog._1h_WatchdogThread, Global_24h_Watchdog._1h_WatchdogThread
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() {

		assertTrue(Global_1h_Watchdog.getInstance().getEnabled());
		//assertTrue(Global_1h_Watchdog.getInstance().isAlive());
		
		assertTrue(Global_24h_Watchdog.getInstance().getEnabled());
		//assertTrue(Global_24h_Watchdog.getInstance().isAlive());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called,
	  							the instance of the ComputeEngine_Processing class for processing serialized files is created
	 * Internal variables TBV: 	processing_engine, Sensors_PATH
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_4() {

		assertNotEquals(null, tcpserver_1.getProcessing_engine());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 							sensor instances are created in accordance with the TCPclass attributes and then they are saved in Server_Sensors_LIST
	 * Internal variables TBV: 	Server_Sensors_LIST
	 * External variables TBV: 	SensorImpl.sensorID, SensorImpl.coordinates, SensorImpl.softwareImageID, SensorImpl.sensor_m_history_array_size, 
	  							SensorImpl.sensor_watchdog_scale_factor
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_5() {
		
		SensorImpl temp_sensor = null;	
		
		for (int i = 1 ; i <= tcpserver_1.sensor_coordinates_array.length; i++) {
			temp_sensor = tcpserver_1.getProcessing_engine().searchInServerSensorList(i);
			
			assertEquals(i,											temp_sensor.getSensorID());
			assertEquals(new Point2D.Float(tcpserver_1.sensor_coordinates_array[i-1][0], tcpserver_1.sensor_coordinates_array[i-1][1]),	
																	temp_sensor.getCoordinates());
			assertEquals(tcpserver_1.getMeasurements_limit(),		temp_sensor.getSensor_m_history_array_size());
			assertEquals(tcpserver_1.getWatchdogs_scale_factor(),	temp_sensor.getLocal_watchdog_scale_factor(), 0.001);
			assertEquals(tcpserver_1.softwareImageID,				temp_sensor.getSoftwareImageID());
			
		}
		assertEquals(tcpserver_1.sensor_coordinates_array.length,	tcpserver_1.Server_Sensors_LIST.size());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 							the flags that indicate if the watchdogs have been kicked are set to FALSE in the 1hWatchog and 24hWatchdog timestamp tables
	 * Internal variables TBV: 	_1hWatchog_timestamp_table, _24hWatchog_timestamp_table
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_6() {

		for (int i = 1; i <= tcpserver_1.sensor_coordinates_array.length; i++) {
			
			assertEquals(false,		tcpserver_1.get_1hWatchog_timestamp_table().get()[i-1]);
			assertEquals(false,		tcpserver_1.get_24hWatchog_timestamp_table().get()[i-1]);
		}
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_7
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, the startServer() function is called
	  							as last step in the constructor that trigger communication via a TCP connection with sensors in a dedicated thread
	 * Internal variables TBV: 	serverThread
	 ***********************************************************************************************************/
	@Test
	public void test_run_7() {

		assertNotEquals(null, tcpserver_1.getServerThread());
		assertTrue(tcpserver_1.getServerThread().isAlive());
		assertFalse(tcpserver_1.getServerThread().isInterrupted());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_8
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called multiple times with the same port ID 
	 							without closing the TCPclass instance created on this port, the BindException is thrown
	 * Exceptions thrown TBV:	BindException
	 * Exceptions thrown:		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test(expected = BindException.class)
	public void test_run_8() throws IOException {
		
		tcpserver_1 = new TCPserver(port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the BindException
		assertTrue(false);
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+TCPserverTest.testID+" teardown section:");

	   // Time offset before running the reinitalize_to_default() function
	   Thread.sleep(100);

	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(tcpserver_1);

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);

	   System.out.println("");
	   incrementTestID();
    }

}
