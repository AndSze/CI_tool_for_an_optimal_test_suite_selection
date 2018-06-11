package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;

public class StartTCPserverTest {

	/***********************************************************************************************************
	 * StartTCPserverTest - Class Attributes
	 ***********************************************************************************************************/
	int port_1 = 9876;
	TCPserver temp_TCPserver = null;
	
	String[] testPurpose = { 	"Verify that once the getInstance() function for the TCPserver server class, the instance of the TCPserver class is being created with attributes set to their values",
								"Verify that if the getInstance() function for the TCPserver server class is being called for the second time, the instance of the TCPserver class is being overwritten with TCPserver that is able to handle TCP communication",
								"Verify that if the getInstance() function for the TCPserver server class is being called after closing the previous TCP communication mechanism, the instance of the TCPserver class is being overwritten with up-to-date TCPserver data"};
	static int testID = 1;

	public static void incrementTestID() {
		StartTCPserverTest.testID += 1;
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
	 * Description: Verify that once the getInstance() function for the TCPserver server class, the instance of the TCPserver class is being created with attributes set to their values
	 * Affected external variables: TCPserver_instance, TCPserver.serverSocket, TCPserver.serverThread, TCPserver.serverRunning
	 * Exceptions thrown: IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException{
		
		assertNotEquals(null,		temp_TCPserver);
		assertEquals(	null,		TCPserver.getServerSocket());
		assertEquals(	null,		TCPserver.getServerThread());
		assertFalse(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: test_run_2
	 * Description: Verify that if the getInstance() function for the TCPserver server class is being called for the second time, 
	 				the instance of the TCPserver class is being overwritten with TCPserver that is able to handle TCP communication
	 * Affected external variables: TCPserver_instance, TCPserver.serverSocket, TCPserver.serverThread, TCPserver.serverRunning
	 * Exceptions thrown: IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertNotEquals(null,		TCPserver.getServerSocket());
		assertNotEquals(null,		TCPserver.getServerThread());
		assertTrue(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: test_run_3
	 * Description: Verify that if the getInstance() function for the TCPserver server class is being called after closing the previous TCP communication mechanism,
	 				the instance of the TCPserver class is being overwritten with up-to-date TCPserver data
	 * Affected external variables: TCPserver_instance, TCPserver.serverSocket, TCPserver.serverThread, TCPserver.serverRunning
	 * Exceptions thrown: IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertNotEquals(null,		TCPserver.getServerSocket());
		assertNotEquals(null,		TCPserver.getServerThread());
		assertTrue(TCPserver.get_ServerRunning());
		
		TCPserver temp_TCPserver_prev = TCPserver.getInstance(port_1);
		ServerSocket temp_ServerSocket_prev = TCPserver.getServerSocket();
		Thread temp_ServerThread_prev = TCPserver.getServerThread();
		
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		
		temp_TCPserver = TCPserver.getInstance(port_1);

		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(temp_TCPserver_prev,		temp_TCPserver);
		assertNotEquals(temp_ServerSocket_prev,		TCPserver.getServerSocket());
		assertNotEquals(temp_ServerThread_prev,		TCPserver.getServerThread());
		assertTrue(TCPserver.get_ServerRunning());
	}
	
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+StartTCPserverTest.testID+" teardown section:");
	   	  
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);

	   incrementTestID();
    }
	   
}
