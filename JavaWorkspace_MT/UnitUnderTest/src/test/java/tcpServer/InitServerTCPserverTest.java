package tcpServer;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.net.BindException;

/*
 * 1) Verify that the server can be created successfully at any port
 * 2) Verify that the Bind Exception is returned if there was an attempt to create the server at the same port twice
 * 3) Verify that multiple servers can be created at different ports
 */

public class InitServerTCPserverTest {
	
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	TCPserver tcpserver_2 = null;

	
	String[] testPurpose = { 	"1) Verify that the server can be created successfully at any port",
			 					"2) Verify that the Bind Exception is returned if there was an attempt to create the server at the same port twice",
			 					"3) Verify that multiple servers can be created successfully at different ports"};
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
		
	}
	
	@Test(expected = BindException.class)
	public void test_2() throws IOException {

		tcpserver_1.initServer(port_1);
		assertNotEquals(tcpserver_1.getServerSocket(),null);
		
		tcpserver_2 = new TCPserver();
		tcpserver_2.initServer(port_1);
		
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
		
		assertNotEquals(tcpserver_1.getServerSocket(),tcpserver_2.getServerSocket());
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
