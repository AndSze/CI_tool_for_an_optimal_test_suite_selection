package deliverables;

import static org.junit.Assert.*;
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
import messages.ServerMessage_ACK;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RunTheClientTest {
	
	UUT_TCPclient UUT_TCPclient_1 = null;
    private int port_1 = 9876;
    private int sensor_ID_1 = 1;
    private String serverHostName  = "localhost";
    private Thread testThread = null;
    
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
	
	String[] testPurpose = { "Verify that once the runTheClientTest() function is called, TCP connection is set up by running the initClient() function for the TCPclient class instance",
							 "Verify that once the runTheClientTest() function is called, a dedicated thread that handles TCP connection is started for the TCPclient class instance",
							 "Verify that UnknownHostException is thrown once the runTheClientTest() function is called on a host name that is not allowed to set up a TCP connection",
							 "Verify that ConnectException is thrown once the runTheClientTest() function is called while attempting to connect a socket to a remote address and port. Typically, the connection was refused remotely",
							 "Verify that IOException is thrown once the runTheClientTest() function is called when the bind operation fails, or if the socket is already bound"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		RunTheClientTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		UUT_TCPclient_1 = new UUT_TCPclient(sensor_ID_1, port_1, serverHostName);
		
		// mock Server Socket to enable the Client Socket to establish connection
		mockTCPserverTest = mock(TCPserver.class);
		mockClientSocket = mock(Socket.class);
		mockComputeEngine_Runnable = mock(ComputeEngine_Runnable.class);
		
		tempServerSocket_1 = new ServerSocket();
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		
		/* To avoid "remote deadlock" - there is a need to submit mockComputeEngine_Runnable to ThreadPoolExecutor 
		 * The ObjectInputStream on the client is waiting for the object stream from the server before proceeding, but the server isn't going to send that, 
		 * because its ObjectInputStream is waiting for the header from the client before proceeding... 
		 */
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
		// Mockito.doAnswer - to mock void method to do something (mock the behavior despite being void) - in this case it is used for TCPserver.startServer();
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		System.out.println("\t\tTest Run "+RunTheClientTest.testID+" Purpose:");
		System.out.println(testPurpose[(RunTheClientTest.testID-1)]);
		System.out.println("\t\tTest Run "+RunTheClientTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the runTheClientTest() function is called, 
	 							TCP connection is set up by running the initClient() function for the TCPclient class instance
	 * External variables TBV: 	TCPclient.clientManager, TCPclient.clientSocket, TCPclient.clientRunning
	 * Mocked external methods: TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings({ "static-access" })
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				UUT_TCPclient_1.setINSTANCE(UUT_TCPclient_1.runTheClient(UUT_TCPclient_1.getINSTANCE(), port_1, serverHostName));
			}
		});
		testThread.start();
		Thread.sleep(500);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to update INSTANCE of TCPclient attribute in UUT_TCPclient_1 class
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog() ));
		
		Thread.sleep(100);
				
		assertNotEquals(null,        UUT_TCPclient_1.getINSTANCE().getClientSocket());
		assertNotEquals(null,        UUT_TCPclient_1.getINSTANCE().getClientManager());
		assertTrue(UUT_TCPclient_1.getINSTANCE().isClientRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the runTheClientTest() function is called, 
	 							a dedicated thread that handles TCP connection is started for the TCPclient class instance
	 * External variables TBV: 	TCPclient.clientThread
	 * Mocked external methods: TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings({ "static-access" })
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				UUT_TCPclient_1.setINSTANCE(UUT_TCPclient_1.runTheClient(UUT_TCPclient_1.getINSTANCE(), port_1, serverHostName));
			}
		});
		testThread.start();
		Thread.sleep(500);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to update INSTANCE of TCPclient attribute in UUT_TCPclient_1 class
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog() ));
		
		Thread.sleep(100);
		
		String client_thread_name = "TCPclient_Thread";

		assertNotEquals(null,        	 UUT_TCPclient_1.getINSTANCE().getClientThread());
		assertEquals(client_thread_name, UUT_TCPclient_1.getINSTANCE().getClientThread().getName());
	}
	
   @SuppressWarnings("static-access")
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+RunTheClientTest.testID+" teardown section:");
	   
	   if(mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if (UUT_TCPclient_1.getINSTANCE().getClientManager() != null) {
		   UUT_TCPclient_1.closeTheClientManager(UUT_TCPclient_1.getINSTANCE());
	   }
	   if (UUT_TCPclient_1.getINSTANCE().getClientSocket() != null) {
		   UUT_TCPclient_1.closeTheClient(UUT_TCPclient_1.getINSTANCE());
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
}
