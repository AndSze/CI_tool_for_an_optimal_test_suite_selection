package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;

public class ClientSocketTCPclientTest {

	int port_1 = 9876;
	TCPclient tcpclient_1 = null;
	int port_2 = 9877;
	TCPclient tcpclient_2 = null;
	final String serverHostName = "localhost";
	TCPserver mockTCPserverTest_1 = null;
	ServerSocket tempServerSocket_1 = null;

	
	String[] testPurpose = { 	"Verify that once a Client instance is created, the client socket for its instance is connected to the specified port number on the named host",
								"Verify that once a Client instance is created, the client socket for its instance is being bound to some local port that has different port number than the port number specified on the named host",
								"Verify that once multiple Client instances are created at the same port, each client instance has its uniquie Client Socket",
								"Verify that if a Client instance at some port was closed, in case the new client instance at this port is created, it will have different socket ID"};
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
		
		if(CloseClientTCPclientTest.testID == 3) {
			tcpclient_2 = new TCPclient();
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
		assertEquals(port_1,		tcpclient_1.getClientSocket().getPort());
	}
	
	@Test
	public void test_run_2() throws IOException {
		
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		assertTrue(tcpclient_1.getClientSocket().isBound());
		assertNotEquals(port_1,		tcpclient_1.getClientSocket().getLocalPort());
	}
	
	@Test
	public void test_run_3() throws IOException {
	
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		 
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_2 = tcpclient_2.initClient(serverHostName, port_1);
		assertTrue(tcpclient_2.isClientRunning());
		assertFalse(tcpclient_2.getClientSocket().isClosed());
		
		Socket tcpclient_1_Socket = tcpclient_1.getClientSocket();
		Socket tcpclient_2_Socket = tcpclient_2.getClientSocket();

		assertNotEquals(tcpclient_1_Socket,		tcpclient_2_Socket);
			
	}
	
	@Test
	public void test_run_4() throws IOException {

		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		 
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		Socket oldSocket = tcpclient_1.getClientSocket();
		
		tcpclient_1.closeClient(tcpclient_1, port_1);
		
		tcpclient_1 = tcpclient_1.initClient(serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		Socket newSocket = tcpclient_1.getClientSocket();

		assertNotEquals(oldSocket,		newSocket);
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
	   
	   incrementTestID();
    }
	
	
}

