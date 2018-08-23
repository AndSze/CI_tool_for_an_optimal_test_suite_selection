package tcpClient;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import watchdog.Local_1h_Watchdog;

public class PublicTCPclientTest {
	
	TCPclient tcpclient_1 = null;
	TCPclient tcpclient_2 = null;
	
	String[] testPurpose = { "Verify that once the default constructor of the TCPclient class is being called for the first time, Client_Sensors_LIST is created",
							 "Verify that once the default constructor of the TCPclient class is being called multiple times, Client_Sensors_LIST from first call of the constructor is used",
							 "Verify that once the default constructor of the TCPclient class is being called for the first time, instance of Local_1h_Watchdog is created",
							 "Verify that once the default constructor of the TCPclient class is being called multiple times, Local_1h_Watchdog instance from first call of the constructor is used"};
	static int testID = 1;
	
	public static void incrementTestID() {
		PublicTCPclientTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpclient_1 = new TCPclient();
		
		if(PublicTCPclientTest.testID == 4) {
			Local_1h_Watchdog.getInstance().setEnabled(true);
		}
		
		System.out.println("\t\tTest Run "+PublicTCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(PublicTCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+PublicTCPclientTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called for the first time, Client_Sensors_LIST is created
	 * Internal variables TBV: 	Client_Sensors_LIST
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() {
		
		assertNotEquals(null, 	tcpclient_1.Client_Sensors_LIST);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called multiple times, Client_Sensors_LIST from first call of the constructor is used
	 * Internal variables TBV: 	Client_Sensors_LIST
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {
		
		assertEquals(tcpclient_2.Client_Sensors_LIST, 	tcpclient_1.Client_Sensors_LIST);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called for the first time, instance of Local_1h_Watchdog is created
	 * Internal variables TBV: 	Local_1h_Watchdog.isPaused, Local_1h_Watchdog._1h_WatchdogThread
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() {
		
		assertNotEquals(null, 	Local_1h_Watchdog.getInstance());
		assertFalse(Local_1h_Watchdog.getInstance().getEnabled());
		assertTrue(Local_1h_Watchdog.getInstance().isAlive());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called multiple times, Local_1h_Watchdog instance from first call of the constructor is used
	 * Internal variables TBV: 	Local_1h_Watchdog.isPaused, Local_1h_Watchdog._1h_WatchdogThread, Local_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws InterruptedException {
		
		// to prove that 1h_watchdog is decreasing
		Thread.sleep(100);
		
		Thread prev_thread = Local_1h_Watchdog.getInstance().get_1h_WatchdogThread();
		double prev = Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		
		tcpclient_2 = new TCPclient();
		
		Thread curr_thread = Local_1h_Watchdog.getInstance().get_1h_WatchdogThread();
		double curr = Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		
		assertEquals(prev,			curr, 0.001);
		assertEquals(prev_thread,	curr_thread);
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+PublicTCPclientTest.testID+" teardown section:");
	   
	   if(Local_1h_Watchdog.getInstance() != null) {
		   Local_1h_Watchdog.getInstance().setM_instance(null);
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
