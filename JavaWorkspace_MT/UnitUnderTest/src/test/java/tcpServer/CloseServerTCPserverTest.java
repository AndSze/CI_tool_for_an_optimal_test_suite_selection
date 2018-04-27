package tcpServer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.BindException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CloseServerTCPserverTest {

	
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	TCPserver tcpserver_2 = null;

	
	String[] testPurpose = { 	"1) Verify that once the server is closed, the new server instance can be initiated at the same port as previous",
			 					"2) Verify that the IO Exception is returned if there was an attempt to close the server that has not been initiated",
			 					"3) Verify that if a server instance at some port is being closed, the other servers at different ports are not being closed"};
	int testID = 0;
	
	
	public void incrementTestID() {
		this.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		System.out.println(testPurpose[testID]);
	}
	

	@Test
	public void test_1() throws IOException {

		tcpserver_1.initServer(port_1);
		assertNotEquals(tcpserver_1.getServerSocket(),null);
		
		tcpserver_1.closeServer(port_1);
		assertEquals(tcpserver_1.getServerSocket(),null);
		
		assertTrue(tcpserver_1.isStopped());
		assertEquals(tcpserver_1.tcpServerInstance(), null);
		
		tcpserver_1.initServer(port_1);
		assertNotEquals(tcpserver_1.getServerSocket(),null);
		
		assertTrue(true);
		
		
	}
	
	@Test(expected = IOException.class)
	public void test_2() throws IOException {

		tcpserver_1.closeServer(port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the BindException
		assertTrue(false);
		
	}
	
	@Test
	public void test_3() throws IOException {

		tcpserver_1.initServer(port_1);
		assertNotEquals(tcpserver_1.getServerSocket(),null);
		
		tcpserver_2 = new TCPserver();
		tcpserver_2.initServer(port_2);
		assertNotEquals(tcpserver_2.getServerSocket(),null);
		
		tcpserver_1.closeServer(port_1);
		assertEquals(tcpserver_1.getServerSocket(),null);
		assertNotEquals(tcpserver_2.getServerSocket(),null);
		
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(tcpserver_1!=null) {
		   tcpserver_1.closeServer(port_1);
	   }
	   if(tcpserver_2!=null) {
		   tcpserver_2.closeServer(port_2);
	   }
	   System.out.println("");
	   incrementTestID();
    }
	
	
}
