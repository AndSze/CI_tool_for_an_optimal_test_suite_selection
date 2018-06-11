package tcpServer;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;
import watchdog.Global_1h_Watchdog;

public class CloseServerTCPserverTest {
	
	/***********************************************************************************************************
	 * CloseServerTCPserverTest - Class Attributes
	 ***********************************************************************************************************/
	int port_1 = 9876;
	TCPserver tempTCPserver = null;

	String[] testPurpose = { "Verify that that once the previously created TCPserver instance is being closed, this instance is being overwritten with up-to-date attributes that indicate TCP communication is not active",
							 "Verify that that once the previously created TCPserver instance is being closed intentionally, 1h_Watchdog TimeLeftBeforeExpiration is being set to its initial limit (_1h_WatchdogExpiration)",
							 "Verify that there is the IllegalArgumentException returned if there was an attempt to close the TCPsever instance that has been already closed"}; 
	static int testID = 1;

	public static void incrementTestID() {
		CloseServerTCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		if (CloseServerTCPserverTest.testID < 3) {
			TCPserver.getInstance(port_1);
		}
		if (CloseServerTCPserverTest.testID == 3) {
			tempTCPserver = TCPserver.getInstance(port_1);
		}
		
		System.out.println("\t\tTest Run "+CloseServerTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseServerTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseServerTCPserverTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description:				Verify that that once the previously created TCPserver instance is being closed, 
					 			this instance is being overwritten with up-to-date attributes that indicate TCP communication is not active
	 * Internal variables TBV: 	TCPserver_instance, TCPserver.serverSocket, TCPserver.serverThread, TCPserver.serverRunning
	 * Exceptions thrown:		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException {
		
		TCPserver.getInstance(port_1);
		
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		
		assertTrue(TCPserver.getServerThread().isInterrupted());
		assertTrue(TCPserver.getServerSocket().isClosed());
		assertFalse(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that that once the previously created TCPserver instance is being closed intentionally, 
	 							1h_Watchdog TimeLeftBeforeExpiration is being set to its initial limit (_1h_WatchdogExpiration)
	 * External variables TBV: 	Global_1h_Watchdog.millisecondsLeftUntilExpiration
	 * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		TCPserver.getInstance(port_1);
		double prev = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		double temp_1h_WatchdogExpiration =  Global_1h_Watchdog.getInstance().getExpiration() * TCPserver.getWatchdogs_scale_factor() ;
		
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		double curr_1 = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(); 
		
		assertEquals( temp_1h_WatchdogExpiration,	 curr_1, 0.001);
		assertTrue("Current 1h_Watchdog TimeLeftBeforeExpiration (" + curr_1 + ") should be greater than previous (" + prev + ")", curr_1 > prev);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that there is the IllegalArgumentException returned if there was an attempt to close the TCPsever instance that has been already closed
	 * Exceptions thrown TBV:	IllegalArgumentException
	 * Exceptions thrown:		IOException
	 ***********************************************************************************************************/
	@Test(expected = IllegalArgumentException.class)
	public void test_run_3() throws IOException {
		
		tempTCPserver = TCPserver.getInstance(port_1);
		
		tempTCPserver.closeServer(tempTCPserver, port_1);
		
		tempTCPserver.closeServer(tempTCPserver, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the IllegalArgumentException
		assertTrue(false);
	}

	@After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+CloseServerTCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);

	   incrementTestID();
    }
}
