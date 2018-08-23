package deliverables;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpClient.Delays;
import tcpClient.Watchdog_Thresholds;


public class UUT_TCPclientTest {
	
	UUT_TCPclient UUT_TCPclient_1 = null;
    private int port_1 = 9876;
    private int sensor_ID_1 = 1;
    private String serverHostName  = "localhost";
	
	String[] testPurpose = { "Verify that once the default constructor of the UUT_TCPclient class is called, all class attributes are updated based on the input arguments",
							 "Verify that once the default constructor of the UUT_TCPclient class is called, the TCPclient class instance is created with a respective sensor_ID",
							 "Verify that once the default constructor of the UUT_TCPclient class is called, the interfaces that parameterize times dependencies for the testing purposes are initiated to its default value",
							 "Verify that once the default constructor of the UUT_TCPclient class is called, there is no attempt to set up connection via TCP, hence attributes of the TCPclass responsible for connection are not initialized"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		UUT_TCPclientTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		UUT_TCPclient_1 = new UUT_TCPclient(sensor_ID_1, port_1, serverHostName);
		
		System.out.println("\t\tTest Run "+UUT_TCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(UUT_TCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+UUT_TCPclientTest.testID+" Logic:");
	}
	
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the default constructor of the UUT_TCPclient class is called, 
	                            all class attributes are updated based on the input arguments
	 * Internal variables TBV: 	port, sensor_ID, serverHostName, INSTANCE
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() {

		assertEquals(port_1,         UUT_TCPclient_1.getPort());
		assertEquals(sensor_ID_1,    UUT_TCPclient_1.getSensor_ID());
		assertEquals(serverHostName, UUT_TCPclient_1.getServerHostName());
		assertNotEquals(null,        UUT_TCPclient_1.getINSTANCE());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the default constructor of the UUT_TCPclient class is called, 
	                            the TCPclient class instance is created with a respective sensor_ID
	 * External variables TBV: 	TCPclient.sensor_ID
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() {

		assertEquals(sensor_ID_1,    UUT_TCPclient_1.getINSTANCE().getSensor_ID());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the default constructor of the UUT_TCPclient class is called, 
	                            the interfaces that parameterize times dependencies for the testing purposes are initiated to its default value
	 * Internal variables TBV: 	watchdog_thresholds_array, delays_array
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() {

		int temp_delays_array_0 = 1000;
		int temp_delays_array_1 = 10000;
		int temp_delays_array_2 = 100000;
		int temp_delays_array_3 = 1000000;
		double temp_watchdog_thresholds_array_0 = 100;
		double temp_watchdog_thresholds_array_1 = 120;
		double temp_watchdog_thresholds_array_2 = 300;
		double temp_watchdog_thresholds_array_3 = 900;
		
		assertEquals(temp_watchdog_thresholds_array_0,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.LOWEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_1,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_2,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGH, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_3,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_delays_array_0,    			UUT_TCPclient.get_delays(Delays.LOWEST, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_1,    			UUT_TCPclient.get_delays(Delays.LOW, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_2,    			UUT_TCPclient.get_delays(Delays.MEDIUM, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_3,    			UUT_TCPclient.get_delays(Delays.HIGHEST, UUT_TCPclient.delays_array));
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the default constructor of the UUT_TCPclient class is called, 
	                            there is no attempt to set up connection via TCP, hence attributes of the TCPclass responsible for connection are not initialized
	 * External variables TBV: 	TCPclient.clientManager, TCPclient.clientSocket, TCPclient.clientRunning
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() {

		assertEquals(null,    UUT_TCPclient_1.getINSTANCE().getClientSocket());
		assertEquals(null,    UUT_TCPclient_1.getINSTANCE().getClientManager());
		assertFalse(UUT_TCPclient_1.getINSTANCE().isClientRunning());
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+UUT_TCPclientTest.testID+" teardown section:");
	   
	   UUT_TCPclient_1.setINSTANCE(null);
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
	   
}
