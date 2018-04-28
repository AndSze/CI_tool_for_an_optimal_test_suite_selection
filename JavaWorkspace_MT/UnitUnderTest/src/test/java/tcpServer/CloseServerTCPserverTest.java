package tcpServer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CloseServerTCPserverTest {

	
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	TCPserver tcpserver_2 = null;

	
	String[] testPurpose = { 	"Verify that once the previously initialized server is closed, the new server instance can be initialized at the same port as previous",
								"Verify that if a server instance at some port was closed, the other servers at different ports are not being closed",
			 					"Verify that there is NO IllegalArgumentException returned if there was an attempt to close twice the server that has been initiated",
			 					"Verify that there is the IllegalArgumentException returned if there was an attempt to close the server that has not been initialized"};
	static int testID = 1;
	
	public static void incrementTestID() {
		CloseServerTCPserverTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		tcpserver_2 = new TCPserver();

		System.out.println("\t\tTest Run "+CloseServerTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseServerTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseServerTCPserverTest.testID+" Logic:");
	}
	

	@Test
	public void test_run_1() throws IOException {
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());

		tcpserver_1.closeServer(tcpserver_1, port_1);
		assertFalse(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isClosed());
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());
	}
	
	@Test
	public void test_run_2() throws IOException {

		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());
		
		tcpserver_2 = tcpserver_2.initServer(port_2);
		assertTrue(tcpserver_2.isServerRunning());
		assertFalse(tcpserver_2.getServerSocket().isClosed());

		tcpserver_1.closeServer(tcpserver_1, port_1);
		assertFalse(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isClosed());
		assertTrue(tcpserver_2.isServerRunning());
		assertFalse(tcpserver_2.getServerSocket().isClosed());
			
	}
	
	@Test
	public void test_run_3() throws IOException {

		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());

		tcpserver_1.closeServer(tcpserver_1, port_1);
		assertFalse(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isClosed());
		
		tcpserver_1.closeServer(tcpserver_1, port_1);
		assertFalse(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isClosed());
			
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void test_run_4() throws IOException {
		
		
		tcpserver_1.closeServer(tcpserver_1, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the IllegalArgumentException
		assertTrue(false);
			
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+CloseServerTCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(tcpserver_1 != null) {
		   if(tcpserver_1.isServerRunning()){
			   tcpserver_1.closeServer(tcpserver_1, port_1);
		   }
		   
	   }
	   if(tcpserver_2 != null){
		   if(tcpserver_2.isServerRunning()){
			   tcpserver_2.closeServer(tcpserver_2, port_2);
		   
		   }
	   }
	   
	   incrementTestID();
    }
	
	
}
