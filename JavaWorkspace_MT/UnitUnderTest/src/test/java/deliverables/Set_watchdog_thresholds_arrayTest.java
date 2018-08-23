package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpClient.Watchdog_Thresholds;

public class Set_watchdog_thresholds_arrayTest {

	UUT_TCPclient UUT_TCPclient_1 = null;
    private int port_1 = 9876;
    private int sensor_ID_1 = 1;
    private String serverHostName  = "localhost";
    private final static int watchdog_thresholds_array_size = 4;
	
	String[] testPurpose = { "Verify default watchdog threshold values in watchdog_thresholds_array. This array has default values if watchdog_scale_factor equals 1",
							 "Verify that watchdog threshold values in watchdog_thresholds_array are scaled based on watchdog_scale_factor if it does not equal 1"};

	static int testID = 1;
	
	public static void incrementTestID() {
		Set_watchdog_thresholds_arrayTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		UUT_TCPclient_1 = new UUT_TCPclient(sensor_ID_1, port_1, serverHostName);
		
		System.out.println("\t\tTest Run "+Set_watchdog_thresholds_arrayTest.testID+" Purpose:");
		System.out.println(testPurpose[(Set_watchdog_thresholds_arrayTest.testID-1)]);
		System.out.println("\t\tTest Run "+Set_watchdog_thresholds_arrayTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify default watchdog threshold values in watchdog_thresholds_array. This array has default values if watchdog_scale_factor equals 1
	 * Internal variables TBV: 	watchdog_thresholds_array
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() {
		
		double temp_watchdog_thresholds_array_0 = 100;
		double temp_watchdog_thresholds_array_1 = 120;
		double temp_watchdog_thresholds_array_2 = 300;
		double temp_watchdog_thresholds_array_3 = 900;
		
		assertEquals(temp_watchdog_thresholds_array_0,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.LOWEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_1,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_2,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGH, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_3,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);

	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that watchdog threshold values in watchdog_thresholds_array are scaled based on watchdog_scale_factor if it does not equal 1
	 * Internal variables TBV: 	watchdog_thresholds_array
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {
		
		double temp_watchdog_scale_factor_1 = 0.01;
		
		UUT_TCPclient.watchdog_thresholds_array = UUT_TCPclient_1.set_watchdog_thresholds_array(temp_watchdog_scale_factor_1, watchdog_thresholds_array_size);
		
		double temp_watchdog_thresholds_array_01 = 100 * temp_watchdog_scale_factor_1;
		double temp_watchdog_thresholds_array_11 = 120 * temp_watchdog_scale_factor_1;
		double temp_watchdog_thresholds_array_21 = 300 * temp_watchdog_scale_factor_1;
		double temp_watchdog_thresholds_array_31 = 900 * temp_watchdog_scale_factor_1;
		
		assertEquals(temp_watchdog_thresholds_array_01,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.LOWEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_11,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_21,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGH, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_31,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		
		double temp_watchdog_scale_factor_2 = 0.25;
		
		UUT_TCPclient.watchdog_thresholds_array = UUT_TCPclient_1.set_watchdog_thresholds_array(temp_watchdog_scale_factor_2, watchdog_thresholds_array_size);
		
		double temp_watchdog_thresholds_array_02 = 100 * temp_watchdog_scale_factor_2;
		double temp_watchdog_thresholds_array_12 = 120 * temp_watchdog_scale_factor_2;
		double temp_watchdog_thresholds_array_22 = 300 * temp_watchdog_scale_factor_2;
		double temp_watchdog_thresholds_array_32 = 900 * temp_watchdog_scale_factor_2;
		
		assertEquals(temp_watchdog_thresholds_array_02,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.LOWEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_12,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, UUT_TCPclient.watchdog_thresholds_array), 0.001);
		assertEquals(temp_watchdog_thresholds_array_22,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGH, UUT_TCPclient.watchdog_thresholds_array), 0.001);	
		assertEquals(temp_watchdog_thresholds_array_32,  UUT_TCPclient.get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, UUT_TCPclient.watchdog_thresholds_array), 0.001);
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+Set_watchdog_thresholds_arrayTest.testID+" teardown section:");
	   
	   UUT_TCPclient_1.setINSTANCE(null);
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
