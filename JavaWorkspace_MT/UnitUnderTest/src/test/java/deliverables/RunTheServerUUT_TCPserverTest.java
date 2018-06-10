package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;

public class RunTheServerUUT_TCPserverTest {

	/***********************************************************************************************************
	 * RunTheServerUUT_TCPserverTest - Class Attributes
	 ***********************************************************************************************************/
	int port_1 = 9876;
	UUT_TCPserver uut_TCPserver_1 = null;

	
	String[] testPurpose = { 	"Verify that once the UUT_TCPserver class constructor is called, an empty instance of the TCP server class is created",
								"Verify that the RunTheServer function returns an updated and running instance of the server (in accordance with initServer() method from the TCPserver class) that overwrites the previously created empty instance of the TCP server",
								"Verify that the instance of TCP server class (one of the attributes of the UUT_TCPserver class that is used for calling the closeTheServer function) is being updated with up-to-date connection data each time the setINSTANCE function is being called for this UUT_TCPserver class object"};
						
	static int testID = 1;

	public static void incrementTestID() {
		RunTheServerUUT_TCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		uut_TCPserver_1 = new UUT_TCPserver(port_1);
		
		System.out.println("\t\tTest Run "+RunTheServerUUT_TCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(RunTheServerUUT_TCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+RunTheServerUUT_TCPserverTest.testID+" Logic:");
	}
	 
    /***********************************************************************************************************
	 * Test Name: 
	 * 		test_run_1
	 * Description: 
	 *		Verify that once the UUT_TCPserver class constructor is called, an empty instance of the TCP server class is created
	 * Affected internal variables: 
	 * 		TCPserver_INSTANCE
	 * 		port
	 * Affected external variables: 
	 * 		TCPserver.serverSocket
	 * 		TCPserver.serverThread
	 *		TCPserver.serverRunning
	 * Called internal functions:
	 * 		UUT_TCPserver()
	 * Exceptions thrown:
	 * 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException {
		
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE());
		assertEquals(	null,		uut_TCPserver_1.getINSTANCE().getServerSocket());
		assertEquals(	null,		uut_TCPserver_1.getINSTANCE().getServerThread());
		assertFalse(uut_TCPserver_1.getINSTANCE().get_ServerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 
	 * 		test_run_2
	 * Description: 
	 *		Verify that the RunTheServer function returns an updated and running instance of the server (in accordance with initServer() 
	 *		method from the TCPserver class) that overwrites the previously created empty instance of the TCP server
	 * Affected internal variables: 
	 * 		TCPserver_INSTANCE
	 * 		port
	 * Affected external variables: 
	 * 		TCPserver.serverSocket
	 * 		TCPserver.serverThread
	 * 		TCPserver.serverRunning
	 * Called internal functions:
	 * 		runTheServer()
	 * 		closeTheServer()
	 * 		UUT_TCPserver()
	 * Exceptions thrown:
	 * 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
		
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerSocket());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerThread());
		assertTrue(uut_TCPserver_1.getINSTANCE().get_ServerRunning());
		
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.closeTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
	}
	
	 /***********************************************************************************************************
	 * Test Name: 
	 * 		test_run_3
	 * Description: 
	 *		Verify that the instance of TCP server class (one of the attributes of the UUT_TCPserver class that is used 
	 *		for calling the closeTheServer function) is being updated with up-to-date connection data each time 
	 *		the setINSTANCE function is being called for this UUT_TCPserver class object.
	 * Affected internal variables: 
	 * 		TCPserver_INSTANCE
	 * 		port
	 * Affected external variables: 
	 * 		TCPserver.serverSocket
	 * 		TCPserver.serverThread
	 * 		TCPserver.serverRunning
	 * Called internal functions:
	 * 		runTheServer()
	 * 		closeTheServer()
	 * 		UUT_TCPserver()
	 * Exceptions thrown:
	 * 		IOException
	 ***********************************************************************************************************/
	
	@Test
	public void test_run_3() throws IOException {
		
		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
		
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerSocket());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerThread());
		assertTrue(uut_TCPserver_1.getINSTANCE().get_ServerRunning());
		
		TCPserver tempTCPserver = uut_TCPserver_1.getINSTANCE();
		ServerSocket tempServerSocket = uut_TCPserver_1.getINSTANCE().getServerSocket();
		Thread tempServerThread = uut_TCPserver_1.getINSTANCE().getServerThread();

		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.closeTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
		assertFalse(uut_TCPserver_1.getINSTANCE().get_ServerRunning());
		
		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));

		assertNotEquals(tempTCPserver,		uut_TCPserver_1.getINSTANCE());
		assertNotEquals(tempServerSocket,	uut_TCPserver_1.getINSTANCE().getServerSocket());
		assertNotEquals(tempServerThread,	uut_TCPserver_1.getINSTANCE().getServerThread());
		assertTrue(uut_TCPserver_1.getINSTANCE().get_ServerRunning());
	}
	
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+RunTheServerUUT_TCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);

	   incrementTestID();
    }
	   
}
