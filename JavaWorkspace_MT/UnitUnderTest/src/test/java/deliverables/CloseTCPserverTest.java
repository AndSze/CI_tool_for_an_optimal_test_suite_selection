package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;
import watchdog.Global_1h_Watchdog;

public class CloseTCPserverTest {

	/***********************************************************************************************************
	 * CloseTCPserverTest - Class Attributes
	 ***********************************************************************************************************/
	int port_1 = 9876;
	TCPserver temp_TCPserver = null;

	String[] testPurpose = { "Verify that that once the previously created TCPserver instance is being closed, this instance is being overwritten with up-to-date attributes that indicate TCP communication is not active",
							 "Verify that that once the previously created TCPserver instance is being closed intentionally, 1h_Watchdog TimeLeftBeforeExpiration is being set to its initial limit (_1h_WatchdogExpiration)"};
	static int testID = 1;

	public static void incrementTestID() {
		CloseTCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		System.out.println("\t\tTest Run "+StartTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(StartTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+StartTCPserverTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: test_run_1
	 * Description: Verify that that once the previously created TCPserver instance is being closed, 
	 				this instance is being overwritten with up-to-date attributes that indicate TCP communication is not active
	 * Affected external variables: TCPserver_instance, TCPserver.serverSocket, TCPserver.serverThread, TCPserver.serverRunning
	 * Exceptions thrown: IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		
		assertTrue(TCPserver.getServerThread().isInterrupted());
		assertTrue(TCPserver.getServerSocket().isClosed());
		assertFalse(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: test_run_2
	 * Description: Verify that that once the previously created TCPserver instance is being closed intentionally, 
	 				1h_Watchdog TimeLeftBeforeExpiration is being set to its initial limit (_1h_WatchdogExpiration)
	 * Affected external variables: Global_1h_Watchdog.millisecondsLeftUntilExpiration
	 * Exceptions thrown: IOException
	 ***********************************************************************************************************/

	@Test
	public void test_run_2() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		double prev = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		double temp_1h_WatchdogExpiration =  Global_1h_Watchdog.getInstance().getExpiration() * TCPserver.getWatchdogs_scale_factor() ;
		
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		double curr_1 = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(); 
		
		assertEquals( temp_1h_WatchdogExpiration,	 curr_1, 0.001);
		assertTrue("Current 1h_Watchdog TimeLeftBeforeExpiration (" + curr_1 + ") should be greater than previous (" + prev + ")", curr_1 > prev);
	}

	@After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+CloseTCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);

	   incrementTestID();
    }
}
