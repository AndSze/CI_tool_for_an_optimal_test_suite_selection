package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;

public class CloseClientTCPclientTest {

	int port_1 = 9876;
	TCPclient tcpclient_1 = null;
	int port_2 = 9877;
	TCPclient tcpclient_2 = null;
	final String serverHostName = "localhost";
	TCPserver mockTCPserverTest_1 = null;
	ServerSocket tempServerSocket_1 = null;
	TCPserver mockTCPserverTest_2 = null;
	ServerSocket tempServerSocket_2 = null;

	
	String[] testPurpose = { 	"Verify that once the previously initialized client is closed, the new client instance can be initialized at the same port as previous",
								"Verify that if a client instance at some port was closed, the other clients at different ports are not being closed",
			 					"Verify that there is NO IllegalArgumentException returned if there was an attempt to close twice the client that has been initiated",
			 					"Verify that there is the IllegalArgumentException returned if there was an attempt to close the client that has not been initialized"};
	static int testID = 1;
	
	public static void incrementTestID() {
		CloseClientTCPclientTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpclient_1 = new TCPclient();
		
		// mock Server Socket to enable the Client Socket to establish connection
		mockTCPserverTest_1 = mock(TCPserver.class);
		tempServerSocket_1 = new ServerSocket();
		when(mockTCPserverTest_1.getServerSocket()).thenReturn(tempServerSocket_1);
		
		if (CloseClientTCPclientTest.testID == 2) {
			tcpclient_2 = new TCPclient();
			mockTCPserverTest_2 = mock(TCPserver.class);
			tempServerSocket_2 = new ServerSocket();
			when(mockTCPserverTest_2.getServerSocket()).thenReturn(tempServerSocket_2);
		}

		System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseClientTCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" Logic:");
	}
	

	@Test
	public void test_run_1() throws IOException {
		
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());

		tcpclient_1.closeClient(tcpclient_1, port_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
	}
	
	@Test
	public void test_run_2() throws IOException {

		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest_2.getServerSocket().bind(new java.net.InetSocketAddress(port_2));
		 
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_2 = tcpclient_2.initClient(serverHostName, port_2);
		assertTrue(tcpclient_2.isClientRunning());
		assertFalse(tcpclient_2.getClientSocket().isClosed());

		tcpclient_1.closeClient(tcpclient_1, port_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
		assertTrue(tcpclient_2.isClientRunning());
		assertFalse(tcpclient_2.getClientSocket().isClosed());
			
	}
	
	@Test
	public void test_run_3() throws IOException {
	
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));

		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());

		tcpclient_1.closeClient(tcpclient_1, port_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_1.closeClient(tcpclient_1, port_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
			
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void test_run_4() throws IOException {
		
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		tcpclient_1.closeClient(tcpclient_1, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the IllegalArgumentException
		assertTrue(false);
			
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(tcpclient_1 != null) {
		   if(tcpclient_1.isClientRunning()){
			   tcpclient_1.closeClient(tcpclient_1, port_1);
		   }
	   }
	   if(tcpclient_2 != null){
		   if(tcpclient_2.isClientRunning()){
			   tcpclient_2.closeClient(tcpclient_2, port_2);
		   
		   }
	   }
	   if(mockTCPserverTest_1 != null){
		   if(!mockTCPserverTest_1.getServerSocket().isClosed()) {
			   mockTCPserverTest_1.getServerSocket().close();
		   }
	   }
	   if(mockTCPserverTest_2 != null){
		   if(!mockTCPserverTest_2.getServerSocket().isClosed()) {
			   mockTCPserverTest_2.getServerSocket().close();
		   }
	   }
	   
	   incrementTestID();
    }
	
	
}

