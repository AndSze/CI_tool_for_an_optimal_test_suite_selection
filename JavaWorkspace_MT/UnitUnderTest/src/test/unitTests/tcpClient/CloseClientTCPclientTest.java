package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;

public class CloseClientTCPclientTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	TCPclient tcpclient_1 = null;
	int port_2 = 9877;
	int sensor_ID_2 = 2;
	TCPclient tcpclient_2 = null;
	final String serverHostName = "localhost";

	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket_1 = null;
	ServerSocket tempServerSocket_2 = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	
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
		mockTCPserverTest = mock(TCPserver.class);
		mockClientSocket = mock(Socket.class);
		
		tempServerSocket_1 = new ServerSocket();
		
		if ( CloseClientTCPclientTest.testID == 2) {
			tcpclient_2 = new TCPclient();
			tempServerSocket_2 = new ServerSocket();
			
			when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1).thenReturn(tempServerSocket_1).thenReturn(tempServerSocket_2).thenReturn(tempServerSocket_2)
													 .thenReturn(tempServerSocket_1).thenReturn(tempServerSocket_1).thenReturn(tempServerSocket_2).thenReturn(tempServerSocket_2); // teardown section
		}
		else {
			when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		}
		
		// Mockito.doAnswer - to mock void method to do something (mock the behavior despite being void) - in this case it is used for TCPserver.startServer();
		// the test uses this approach for the purpose of avoiding actual messages sent via TCP - it will be checked in the integration tests
		Mockito.doAnswer(new Answer<Thread>() {
            @Override
            public Thread answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                if (arguments != null && arguments.length > 0 && arguments[0] != null ) {
                	final ServerSocket servSocket = (ServerSocket) arguments[0];
                	mockServerThread = new Thread(new Runnable() {
                		public void run() {
                			while(!servSocket.isClosed()) {
		                		try {
									mockClientSocket = servSocket.accept();
									mockComputeEngine_Runnable = Mockito.spy(new ComputeEngine_Runnable(mockClientSocket, 1.0, false));
									auxiliaryServerThreadExecutor.submit(mockComputeEngine_Runnable);
								} catch (IOException IOex) {
									mockServerThread.interrupt();
									System.out.println("Server Thread Stopped.");
									System.out.println("Server" + IOex.getMessage());
									break;
								}
                			}
                		}
                	});
                }
                return mockServerThread;
            }
		}).when(mockTCPserverTest).startServer(Matchers.any(ServerSocket.class));

		System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseClientTCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" Logic:");
	}
	

	@Test
	public void test_run_1() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());

		tcpclient_1.closeClient(tcpclient_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
	}
	
	@Test
	public void test_run_2() throws IOException {

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_2));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		 
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_2, serverHostName, port_2);
		assertTrue(tcpclient_2.isClientRunning());
		assertFalse(tcpclient_2.getClientSocket().isClosed());

		tcpclient_1.closeClient(tcpclient_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
		assertTrue(tcpclient_2.isClientRunning());
		assertFalse(tcpclient_2.getClientSocket().isClosed());
			
	}
	
	@Test
	public void test_run_3() throws IOException {
	
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();

		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());

		tcpclient_1.closeClient(tcpclient_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_1.closeClient(tcpclient_1);
		assertFalse(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_1.getClientSocket().isClosed());
			
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void test_run_4() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1.closeClient(tcpclient_1);
		
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
			   tcpclient_1.closeClient(tcpclient_1);
		   }
	   }
	   if(tcpclient_2 != null){
		   if(tcpclient_2.isClientRunning()){
			   tcpclient_2.closeClient(tcpclient_2);
		   
		   }
	   }
	   if(mockTCPserverTest != null){
		   if(!mockTCPserverTest.getServerSocket().isClosed()) {
			   mockTCPserverTest.getServerSocket().close();
		   }
		   if(!mockTCPserverTest.getServerSocket().isClosed()) {
			   mockTCPserverTest.getServerSocket().close();
		   }
	   }
	   	  
	   
	   incrementTestID();
    }
	
	
}

