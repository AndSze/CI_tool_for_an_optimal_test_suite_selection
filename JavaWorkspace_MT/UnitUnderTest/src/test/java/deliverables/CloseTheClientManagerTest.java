package deliverables;

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
import tcpClient.TCPclient;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;

public class CloseTheClientManagerTest {
	
	UUT_TCPclient UUT_TCPclient_1 = null;
	TCPclient tcp_client_1 = null;
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
	
	String[] testPurpose = { "Verify that once the closeTheClientManager() function is called, client manager with its input and output stream for TCP connection is closed by running the closeClientManager() function for the TCPclient class instance"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		CloseTheClientManagerTest.testID += 1;
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
		
		System.out.println("\t\tTest Run "+CloseTheClientManagerTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseTheClientManagerTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseTheClientManagerTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the closeTheClientManager() function is called, 
	  							client manager with its input and output stream for TCP connection is closed by running the closeClientManager() function for the TCPclient class instance
	 * External variables TBV: 	TCPclient.clientManager
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings({ "static-access" })
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		tcp_client_1 = UUT_TCPclient_1.getINSTANCE();
		tcp_client_1 = UUT_TCPclient_1.getINSTANCE().initClient(sensor_ID_1, serverHostName, port_1);
		UUT_TCPclient_1.setINSTANCE(tcp_client_1);
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					Thread TCPclient_thread = new Thread(UUT_TCPclient_1.getINSTANCE(), "TCPclient Thread");
					TCPclient_thread.run();
				} catch (NullPointerException NPE) {
					// DO NOTHING - it is impossible to prevent from this exception being thrown due to exception handling logic for ClientManager
				}
				
			}
		});
		testThread.start();
		Thread.sleep(100);
		
		assertTrue(UUT_TCPclient_1.getINSTANCE().getClientManager().isClientManagerRunning());
		assertTrue(UUT_TCPclient_1.getINSTANCE().isClientRunning());
		
		UUT_TCPclient_1.setINSTANCE(UUT_TCPclient_1.closeTheClientManager(UUT_TCPclient_1.getINSTANCE()));
				
		assertFalse(UUT_TCPclient_1.getINSTANCE().getClientManager().isClientManagerRunning());
		assertTrue(UUT_TCPclient_1.getINSTANCE().isClientRunning());
	}
		
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+CloseTheClientManagerTest.testID+" teardown section:");
	   
	   UUT_TCPclient_1.setINSTANCE(null);
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
