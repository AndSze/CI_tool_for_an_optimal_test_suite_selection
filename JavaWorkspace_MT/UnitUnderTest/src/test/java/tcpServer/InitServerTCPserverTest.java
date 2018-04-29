package tcpServer;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.net.BindException;

public class InitServerTCPserverTest {
	
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	TCPserver tcpserver_2 = null;

	
	String[] testPurpose = { 	"Verify that the server can be created successfully at any port",
			 					"Verify that the Bind Exception is returned if there was an attempt to create the server at the same port twice",
			 					"Verify that multiple servers can be created successfully at different ports"};
	static int testID = 1;
	
	
	
	public static void incrementTestID() {
		InitServerTCPserverTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		tcpserver_2 = new TCPserver();

		System.out.println("\t\tTest Run "+InitServerTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(InitServerTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+InitServerTCPserverTest.testID+" Logic:");
	}
	

	@Test
	public void test_run_1() throws IOException {

		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		
	}
	
	@Test(expected = BindException.class)
	public void test_run_2() throws IOException {

		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		
		tcpserver_2 = new TCPserver();
		tcpserver_2 = tcpserver_2.initServer(port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the BindException
		assertTrue(false);
		
	}
	
	@Test
	public void test_run_3() throws IOException {

		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		
		tcpserver_2 = tcpserver_2.initServer(port_2);
		assertTrue(tcpserver_2.isServerRunning());
		
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+InitServerTCPserverTest.testID+" teardown section:");
	   
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
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   System.out.println("");
	   incrementTestID();
    }
	
	
}
