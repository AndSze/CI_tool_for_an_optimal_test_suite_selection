package tcpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class TCPserverTest {
	
	TCPserver tcpserver_1 = null;
	int port_1 = 9876;
	
	String[] testPurpose = { "Verify that once the overloaded constructor of the TCPserver class is called, the TCPserver class instance is updated with new server socket that is bound to the port and has ReuseAddress set to TRUE",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the TCPserver class instance is updated with the serverRunning flag set to TRUE",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the 1h_Watchdog and 24h_Watchdog instances are enabled along with their threads",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the instance of the ComputeEngine_Processing class for processing of the serialized files is created and the deleteAllFilesFromDirectiory is called",
							 "Verify that once the default constructor of the TCPserver class is being called, the 1h_Watchdog and 24h_Watchdog instances are being created with its local attribute server_watchgod_scale_factor that is equal to TCPserver.watchdogs_scale_factor",
							 "Verify that the default constructor of the TCPserver class can be called multiple times with no exception being thrown. Verify that after each constructor call, the TCPserver instance is being updated with up-to-date attributes, e.g. watchdogs_scale_factors"};
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
	@SuppressWarnings("static-access")
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
		assertTrue(Global_1h_Watchdog.getInstance().isAlive());
		
		assertTrue(Global_24h_Watchdog.getInstance().getEnabled());
		assertTrue(Global_24h_Watchdog.getInstance().isAlive());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 * 							the instance of the ComputeEngine_Processing class for processing serialized files is created and the deleteAllFilesFromDirectiory is called
	 * Internal variables TBV: 	processing_engine, Sensors_PATH
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_4() {

		assertNotEquals(null, tcpserver_1.getProcessing_engine());
		
		String new_path = null;
		File file_path = null;
		File files_to_be_deleted_path = null;
		file_path = new java.io.File(tcpserver_1.Sensors_PATH);
		String[]entries_1 = file_path.list();
		for (int i = 0; i< entries_1.length; i++) {
			new_path = tcpserver_1.Sensors_PATH + "\\" + entries_1[i];
			file_path = new java.io.File(new_path);
			String[]entries_2 = file_path.list();
			for (int j = 0; j< entries_2.length; j++) {
				new_path = tcpserver_1.Sensors_PATH  + "\\" + entries_1[i] + "\\" + entries_2[j];
				files_to_be_deleted_path = new java.io.File(new_path);
				// the sensor_Infos files are created once the overloaded constructor of the TCPserver class is called, 
				// hence there will be new sensor_Infos files created after deleting all files from previous TCP communication session, 
				// hence if a folder name equals "sensor_Infos", the assertEquals condition is not executed for this for loop iteration
				if(entries_2[j].equals("sensor_Infos")) continue;
				// if an array returned by listFiles() is empty, an evidence that all files have been deleted is given
				assertEquals(0, files_to_be_deleted_path.listFiles());
			}
		}
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
		assertEquals(Global_24h_Watchdog.getServer_watchgod_scale_factor(), TCPserver.getWatchdogs_scale_factor(), 0.0001);
		
		assertNotEquals(null, tcpserver_1);
		
		tcpserver_1 = new TCPserver();
		double temp_watchdogs_scale_factor = 0.01;
		TCPserver.setWatchdogs_scale_factor(temp_watchdogs_scale_factor);
		TCPserver temp_TCPserver_2 = tcpserver_1;
		
		assertNotEquals(temp_TCPserver_1, temp_TCPserver_2);
		assertEquals(Global_1h_Watchdog.getServer_watchgod_scale_factor(), temp_watchdogs_scale_factor, 0.0001);
		assertEquals(Global_24h_Watchdog.getServer_watchgod_scale_factor(), temp_watchdogs_scale_factor, 0.0001);	
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+TCPserverTest.testID+" teardown section:");
	   
	   tcpserver_1.closeServer(tcpserver_1, port_1);
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
