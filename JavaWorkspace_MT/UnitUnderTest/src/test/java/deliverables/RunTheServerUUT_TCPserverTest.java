package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tcpClient.initClientTCPclientTest;
import tcpServer.TCPserver;

public class RunTheServerUUT_TCPserverTest {

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
	
	@Test
	public void test_run_1() throws IOException {
		
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE());
		assertEquals(	null,		uut_TCPserver_1.getINSTANCE().getServerSocket());
		assertEquals(	null,		uut_TCPserver_1.getINSTANCE().getServerThread());
		assertFalse(uut_TCPserver_1.getINSTANCE().isServerRunning());
	}
	
	@Test
	public void test_run_2() throws IOException {
		
		TCPserver tcp_server_temp = UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort());
		
		assertNotEquals(null,		tcp_server_temp);
		assertNotEquals(null,		tcp_server_temp.getServerSocket());
		assertNotEquals(null,		tcp_server_temp.getServerThread());
		assertTrue(tcp_server_temp.isServerRunning());
		
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		tcp_server_temp.closeServer(tcp_server_temp, port_1);
	}
	
	@Test
	public void test_run_3() throws IOException {
		
		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
		
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerSocket());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerThread());
		assertTrue(uut_TCPserver_1.getINSTANCE().isServerRunning());
	}
	
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+RunTheServerUUT_TCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);

	   incrementTestID();
    }
	   
}
