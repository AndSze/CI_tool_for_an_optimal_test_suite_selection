package tcpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class PublicTCPserverTest {
	
	TCPserver tcpserver_1 = null;
	
	String[] testPurpose = { "Verify that once the default constructor of the TCPserver class is being called, the TCPserver class instance is created with attributes set to their initial values",
							 "Verify that once the default constructor of the TCPserver class is being called, the array lists that temporarily store sensors and measurements are created",
							 "Verify that once the default constructor of the TCPserver class is being called, sensor_coordinates_array has number of elements equals to number of elements in_Watchog_timestamp_tables",
							 "Verify that once the default constructor of the TCPserver class is being called, the 1h_Watchdog and 24h_Watchdog instances are being created with its local attribute server_watchgod_scale_factor that is equal to TCPserver.watchdogs_scale_factor",
							 "Verify that the default constructor of the TCPserver class can be called multiple times with no exception being thrown. Verify that after each constructor call, the TCPserver instance is being updated with up-to-date attributes, e.g. watchdogs_scale_factors"};
	static int testID = 1;
	
	public static void incrementTestID() {
		PublicTCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpserver_1 = new TCPserver();
		
		System.out.println("\t\tTest Run "+PublicTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(PublicTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+PublicTCPserverTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the default constructor of the TCPserver class is being called, the TCPserver class instance is created with attributes set to their initial values
	 * Internal variables TBV: 	serverSocket, serverThread, serverRunning
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() {

		assertFalse(TCPserver.get_ServerRunning());
		assertEquals(null, tcpserver_1.getServerSocket());
		assertEquals(null, tcpserver_1.getServerThread());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the default constructor of the TCPserver class is being called, the array lists that temporarily store sensors and measurements are created
	 * Internal variables TBV: 	Server_Sensors_LIST, MeasurementHistory_LIST, MeasurementData_LIST
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() {

		assertEquals(0, TCPserver.Server_Sensors_LIST.size());
		assertEquals(0, TCPserver.MeasurementHistory_LIST.size());
		assertEquals(0, TCPserver.MeasurementData_LIST.size());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the default constructor of the TCPserver class is being called, sensor_coordinates_array has number of elements equals to number of elements in_Watchog_timestamp_tables
	 * Internal variables TBV: 	_1hWatchog_timestamp_table, _24hWatchog_timestamp_table, sensor_coordinates_array
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() {

		assertEquals(tcpserver_1.sensor_coordinates_array.length, TCPserver.get_1hWatchog_timestamp_table().get().length);
		assertEquals(tcpserver_1.sensor_coordinates_array.length, TCPserver.get_24hWatchog_timestamp_table().get().length);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the default constructor of the TCPserver class is being called, the 1h_Watchdog and 24h_Watchdog instances are being created with its local attribute server_watchgod_scale_factor that is equal to TCPserver.watchdogs_scale_factor
	 * Internal variables TBV: 	watchdogs_scale_factor
	 * External variables TBV: 	Global_1h_Watchdog.server_watchgod_scale_factor, Global_24h_Watchdog.server_watchgod_scale_factor
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() {

		assertEquals(Global_1h_Watchdog.getServer_watchgod_scale_factor(), TCPserver.getWatchdogs_scale_factor(), 0.0001);
		assertEquals(Global_24h_Watchdog.getServer_watchdog_scale_factor(), TCPserver.getWatchdogs_scale_factor(), 0.0001);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that the default constructor of the TCPserver class can be called multiple times with no exception being thrown. Verify that after each constructor call, the TCPserver instance is being updated with up-to-date attributes, e.g. watchdogs_scale_factors
	 * Internal variables TBV: 	watchdogs_scale_factor
	 * External variables TBV: 	Global_1h_Watchdog.server_watchgod_scale_factor, Global_24h_Watchdog.server_watchgod_scale_factor
	 * Exceptions thrown:		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException {

		tcpserver_1 = new TCPserver();
		TCPserver temp_TCPserver_1 = tcpserver_1;
		
		assertEquals(Global_1h_Watchdog.getServer_watchgod_scale_factor(), TCPserver.getWatchdogs_scale_factor(), 0.0001);
		assertEquals(Global_24h_Watchdog.getServer_watchdog_scale_factor(), TCPserver.getWatchdogs_scale_factor(), 0.0001);
		
		assertNotEquals(null, tcpserver_1);
		
		tcpserver_1 = new TCPserver();
		double temp_watchdogs_scale_factor = 0.01;
		TCPserver.setWatchdogs_scale_factor(temp_watchdogs_scale_factor);
		TCPserver temp_TCPserver_2 = tcpserver_1;
		
		assertNotEquals(temp_TCPserver_1, temp_TCPserver_2);
		assertEquals(Global_1h_Watchdog.getServer_watchgod_scale_factor(), temp_watchdogs_scale_factor, 0.0001);
		assertEquals(Global_24h_Watchdog.getServer_watchdog_scale_factor(), temp_watchdogs_scale_factor, 0.0001);	
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+PublicTCPserverTest.testID+" teardown section:");
	   
	   if(tcpserver_1.getServerSocket() != null) {
		   if(!tcpserver_1.getServerSocket().isClosed()){
			   tcpserver_1.getServerSocket().close();
		   }
	   }
	   if (tcpserver_1 != null) {
		   tcpserver_1 = null;
	   }
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
