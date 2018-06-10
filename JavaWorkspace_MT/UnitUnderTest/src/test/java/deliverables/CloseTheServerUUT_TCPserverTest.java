package deliverables;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tcpServer.TCPserver;

public class CloseTheServerUUT_TCPserverTest {

	int port_1 = 9876;
	UUT_TCPserver uut_TCPserver_1 = null;

	
	String[] testPurpose = { 	"Verify that the CloseTheServer function returns an updated instance of the server (in accordance with closeServer() method from the TCPserver class) that overwrites the previously created running instance of the TCP server",
								"Verify that the instance of TCP server class (one of the attributes of the UUT_TCPserver class that is used for calling the closeTheServer function) is being updated with up-to-date connection data each time the setINSTANCE function is being called for this UUT_TCPserver class object"};
						
			
	static int testID = 1;

	public static void incrementTestID() {
		CloseTheServerUUT_TCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		uut_TCPserver_1 = new UUT_TCPserver(port_1);
		
		if(CloseTheServerUUT_TCPserverTest.testID == 2) {
			uut_TCPserver_1.setINSTANCE(UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
		}
		
		System.out.println("\t\tTest Run "+CloseTheServerUUT_TCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseTheServerUUT_TCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseTheServerUUT_TCPserverTest.testID+" Logic:");
	}
	
	@Test
	public void test_run_1() throws IOException {
		
		TCPserver tcp_server_temp = UUT_TCPserver.runTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort());
				
		// close the server instance to prevent the Bind Exception from being thrown due to an attempt to bind a server socket twice to the same port
		tcp_server_temp.closeServer(tcp_server_temp, port_1);
		
		assertNotEquals(null,		tcp_server_temp);
		assertNotEquals(null,		tcp_server_temp.getServerThread());
		assertTrue(tcp_server_temp.getServerSocket().isClosed());
		assertFalse(tcp_server_temp.get_ServerRunning());
	}
	
	@Test
	public void test_run_2() throws IOException {
		
		uut_TCPserver_1.setINSTANCE(UUT_TCPserver.closeTheServer(uut_TCPserver_1.getINSTANCE(), uut_TCPserver_1.getPort()));
		
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE());
		assertNotEquals(null,		uut_TCPserver_1.getINSTANCE().getServerThread());
		assertTrue(uut_TCPserver_1.getINSTANCE().getServerSocket().isClosed());
		assertFalse(uut_TCPserver_1.getINSTANCE().get_ServerRunning());
	}
	
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+CloseTheServerUUT_TCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);

	   incrementTestID();
    }
	   
}

