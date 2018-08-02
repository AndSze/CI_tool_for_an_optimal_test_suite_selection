package tcpServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GetInstanceTCPserverTest {
	/***********************************************************************************************************
	 * StartTCPserverTest - Class Attributes
	 ***********************************************************************************************************/
	int port_1 = 9876;
	int port_2 = 9877;
	TCPserver temp_TCPserver = null;
	
	String[] testPurpose = { 	"Verify that once the getInstance() function is called for the first time, the default constructor of the TCPclass is being called and the TCPclass instance with its attributes set to inial values is returned",
								"Verify that if the getInstance() function is called for the second time, the overloaded constructor of the TCPclass is being called and the TCPclass instance with its attributes set to updated values is returned",
								"Verify that if the getInstance() function is called after closing the previous communication via a TCP connection, the overloaded constructor of the TCPclass is being called and the overwritten TCPclass instance with its attributes set to up-to-date values is returned",
								"Verify that if the getInstance() function is called for the TCPserver class instance that has active communication via a TCP connection, this TCPserver class instance is returned",
								"Verify that if the getInstance() function is called with a port number that is different than the port number of the TCPserver class instance that has active communication via a TCP connection, the TCPserver class instance with active communication via a TCP connection is returned"};
	static int testID = 1;

	public static void incrementTestID() {
		GetInstanceTCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		System.out.println("\t\tTest Run "+GetInstanceTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(GetInstanceTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+GetInstanceTCPserverTest.testID+" Logic:");
	}
	 
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the getInstance() function is called for the first time, 
				 				the default constructor of the TCPclass is being called and the TCPclass instance with its attributes set to inial values is returned
	 * Internal variables TBV: 	serverSocket, serverThread, serverRunning
	 * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException{
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertEquals(	null,		temp_TCPserver.getServerSocket());
		assertEquals(	null,		temp_TCPserver.getServerThread());
		assertFalse(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that if the getInstance() function is called for the second time, 
	  							the overloaded constructor of the TCPclass is being called and the TCPclass instance with its attributes set to updated values is returned
	 * Internal variables TBV: 	serverSocket, serverThread, serverRunning
	 * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertNotEquals(null,		temp_TCPserver.getServerSocket());
		assertNotEquals(null,		temp_TCPserver.getServerThread());
		assertTrue(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that if the getInstance() function is called after closing the previous communication via a TCP connection,
	  						 	the overloaded constructor of the TCPclass is being called and the overwritten TCPclass instance with its attributes set to up-to-date values is returned
	 * Internal variables TBV: 	serverSocket, serverThread, serverRunning
	 * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertNotEquals(null,		temp_TCPserver.getServerSocket());
		assertNotEquals(null,		temp_TCPserver.getServerThread());
		assertTrue(TCPserver.get_ServerRunning());
		
		TCPserver temp_TCPserver_prev = TCPserver.getInstance(port_1);
		ServerSocket temp_ServerSocket_prev = temp_TCPserver.getServerSocket();
		Thread temp_ServerThread_prev = temp_TCPserver.getServerThread();
		
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		
		temp_TCPserver = TCPserver.getInstance(port_1);

		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(temp_TCPserver_prev,		temp_TCPserver);
		assertNotEquals(temp_ServerSocket_prev,		temp_TCPserver.getServerSocket());
		assertNotEquals(temp_ServerThread_prev,		temp_TCPserver.getServerThread());
		assertTrue(TCPserver.get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that if the getInstance() function is called for the TCPserver class instance that has active communication via a TCP connection, 
	 							this TCPserver class instance is returned
	 * Internal variables TBV: 	serverSocket, serverThread, serverRunning
	 * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertTrue(TCPserver.get_ServerRunning());
		
		assertEquals(temp_TCPserver.getServerSocket(), 	TCPserver.getInstance(port_1).getServerSocket());
		TCPserver.getInstance(port_1);
		assertEquals(temp_TCPserver, 					TCPserver.getInstance(port_1));
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that if the getInstance() function is called with a port number that is different than the port number of the TCPserver class instance that
	 = 							has active communication via a TCP connection, the TCPserver class instance with active communication via a TCP connection is returned
	 * Internal variables TBV: 	serverSocket, serverThread, serverRunning
	 * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException {
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		temp_TCPserver = TCPserver.getInstance(port_1);
		
		assertNotEquals(null,		temp_TCPserver);
		assertTrue(TCPserver.get_ServerRunning());
		
		assertEquals(temp_TCPserver.getServerSocket(), 	TCPserver.getInstance(port_2).getServerSocket());
		TCPserver.getInstance(port_2);
		assertEquals(temp_TCPserver, 					TCPserver.getInstance(port_2));
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+GetInstanceTCPserverTest.testID+" teardown section:");
	   	  
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		TCPserver.getInstance(port_1).closeServer(TCPserver.getInstance(port_1), port_1);
		
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);

	   incrementTestID();
    }
	   
}
