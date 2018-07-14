package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpClient.Delays;

public class Set_delays_arrayTest {
	
	UUT_TCPclient UUT_TCPclient_1 = null;
    private int port_1 = 9876;
    private int sensor_ID_1 = 1;
    private String serverHostName  = "localhost";
    private final static int delays_array_size = 4;
	
	String[] testPurpose = { "Verify default delays values in delays_array. This array has default values if watchdog_scale_factor equals 1",
							 "Verify that delays values in delays_array are scaled based on watchdog_scale_factor if it does not equal 1"};

	static int testID = 1;
	
	public static void incrementTestID() {
		Set_delays_arrayTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		UUT_TCPclient_1 = new UUT_TCPclient(sensor_ID_1, port_1, serverHostName);
		
		System.out.println("\t\tTest Run "+Set_delays_arrayTest.testID+" Purpose:");
		System.out.println(testPurpose[(Set_delays_arrayTest.testID-1)]);
		System.out.println("\t\tTest Run "+Set_delays_arrayTest.testID+" Logic:");
	}
	
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify default delays values in delays_array. This array has default values if watchdog_scale_factor equals 1
	 * Internal variables TBV: 	delays_array
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() {
		
		int temp_delays_array_0 = 1000;
		int temp_delays_array_1 = 10000;
		int temp_delays_array_2 = 100000;
		int temp_delays_array_3 = 1000000;
		
		assertEquals(temp_delays_array_0,    			UUT_TCPclient.get_delays(Delays.LOWEST, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_1,    			UUT_TCPclient.get_delays(Delays.LOW, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_2,    			UUT_TCPclient.get_delays(Delays.MEDIUM, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_3,    			UUT_TCPclient.get_delays(Delays.HIGHEST, UUT_TCPclient.delays_array));

	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify delays values in delays_array that are scaled based on watchdog_scale_factor if it does not equal 1
	 * Internal variables TBV: 	delays_array
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {
		
		double temp_watchdog_scale_factor_1 = 0.01;
		
		UUT_TCPclient.delays_array = UUT_TCPclient_1.set_delays_array(temp_watchdog_scale_factor_1, delays_array_size);
		
		int temp_delays_array_01 = (int) (1000 * temp_watchdog_scale_factor_1);
		int temp_delays_array_11 = (int) (10000 * temp_watchdog_scale_factor_1);
		int temp_delays_array_21 = (int) (100000 * temp_watchdog_scale_factor_1);
		int temp_delays_array_31 = (int) (1000000 * temp_watchdog_scale_factor_1);
		
		assertEquals(temp_delays_array_01,    			UUT_TCPclient.get_delays(Delays.LOWEST, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_11,    			UUT_TCPclient.get_delays(Delays.LOW, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_21,    			UUT_TCPclient.get_delays(Delays.MEDIUM, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_31,    			UUT_TCPclient.get_delays(Delays.HIGHEST, UUT_TCPclient.delays_array));
		
		double temp_watchdog_scale_factor_2 = 0.25;
		
		UUT_TCPclient.delays_array = UUT_TCPclient_1.set_delays_array(temp_watchdog_scale_factor_2, delays_array_size);
		
		int temp_delays_array_02 = (int) (1000 * temp_watchdog_scale_factor_2);
		int temp_delays_array_12 = (int) (10000 * temp_watchdog_scale_factor_2);
		int temp_delays_array_22 = (int) (100000 * temp_watchdog_scale_factor_2);
		int temp_delays_array_32 = (int) (1000000 * temp_watchdog_scale_factor_2);
		
		assertEquals(temp_delays_array_02,    			UUT_TCPclient.get_delays(Delays.LOWEST, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_12,    			UUT_TCPclient.get_delays(Delays.LOW, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_22,    			UUT_TCPclient.get_delays(Delays.MEDIUM, UUT_TCPclient.delays_array));
		assertEquals(temp_delays_array_32,    			UUT_TCPclient.get_delays(Delays.HIGHEST, UUT_TCPclient.delays_array));

	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+Set_delays_arrayTest.testID+" teardown section:");
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
