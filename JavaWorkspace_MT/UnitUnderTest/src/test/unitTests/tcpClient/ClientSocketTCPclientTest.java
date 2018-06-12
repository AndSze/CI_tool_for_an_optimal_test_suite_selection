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

public class ClientSocketTCPclientTest {

	int port_1 = 9876;
	int sensor_ID = 1;
	TCPclient tcpclient_1 = null;
	int port_2 = 9877;
	TCPclient tcpclient_2 = null;
	final String serverHostName = "localhost";
	
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	
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
		mockTCPserverTest = mock(TCPserver.class);
		mockClientSocket = mock(Socket.class);
		
		tempServerSocket = new ServerSocket();
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket);
		
		
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

		if(CloseClientTCPclientTest.testID == 3) {
			tcpclient_2 = new TCPclient();
		}
		
		System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseClientTCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseClientTCPclientTest.testID+" Logic:");
	}
	

	@Test
	public void test_run_1() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		assertEquals(port_1,		tcpclient_1.getClientSocket().getPort());
	}
	
	@Test
	public void test_run_2() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		assertTrue(tcpclient_1.getClientSocket().isBound());
		assertNotEquals(port_1,		tcpclient_1.getClientSocket().getLocalPort());
	}
	
	@Test
	public void test_run_3() throws IOException {
	
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		 
		tcpclient_1 = tcpclient_1.initClient(sensor_ID, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		tcpclient_2 = tcpclient_2.initClient(sensor_ID, serverHostName, port_1);
		assertTrue(tcpclient_2.isClientRunning());
		assertFalse(tcpclient_2.getClientSocket().isClosed());
		
		Socket tcpclient_1_Socket = tcpclient_1.getClientSocket();
		Socket tcpclient_2_Socket = tcpclient_2.getClientSocket();

		assertNotEquals(tcpclient_1_Socket,		tcpclient_2_Socket);
			
	}
	
	@Test
	public void test_run_4() throws IOException {

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		 
		tcpclient_1 = tcpclient_1.initClient(sensor_ID, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		
		Socket oldSocket = tcpclient_1.getClientSocket();
		
		tcpclient_1.closeClient(tcpclient_1);
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID, serverHostName, port_1);
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
	   }
	   
	   incrementTestID();
    }
	
	
}

