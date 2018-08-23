package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;

public class ClientManagerTest {

	ClientManager clientManager_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    String serverHostName  = "localhost";
    
    // input and output stream for ClinetManager - they cannot be mocked to prevent the test from being stuck in mockClientSocket = servSocket.accept();
	ObjectOutputStream tempOutputStream = null;
	ObjectInputStream tempInputStream = null;
	Socket TCPclientSocket = null;
    
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket_1 = null;
	TCPclient mockTCPclientTest = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	
	String[] testPurpose = { "Verify that once the overloaded constructor of the ClientManager class is being called, sensor_ID, input and output stream for the ClientManger class instance are set",
							 "Verify that once the overloaded constructor of the ClientManager class is being called, the isClientManagerRunning flag that indicates if  the ClientManger class is active is set to true"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		ClientManagerTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		// mock Server Socket to enable the Client Socket to establish connection
		mockTCPserverTest = mock(TCPserver.class);
		mockTCPclientTest = mock(TCPclient.class);
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
		
		System.out.println("\t\tTest Run "+ClientManagerTest.testID+" Purpose:");
		System.out.println(testPurpose[(ClientManagerTest.testID-1)]);
		System.out.println("\t\tTest Run "+ClientManagerTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the overloaded constructor of the ClientManager class is being called, sensor_ID, input and output stream for the ClientManger class instance are set
	 * Internal variables TBV: 	outputStream, inputStream, sensor_ID
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);
		
		tempOutputStream = new ObjectOutputStream(mockTCPclientTest.getClientSocket().getOutputStream());
		tempInputStream = new ObjectInputStream(mockTCPclientTest.getClientSocket().getInputStream());
		
		clientManager_1 = new ClientManager(tempOutputStream, tempInputStream, sensor_ID_1);
		
		assertEquals(tempInputStream,		clientManager_1.getInputReaderStream());	
		assertEquals(tempOutputStream,		clientManager_1.getOutputStream());		
		assertEquals(sensor_ID_1,			clientManager_1.sensor_ID);	
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the overloaded constructor of the ClientManager class is being called,
	 							the isClientManagerRunning flag that indicates if the ClientManger class is active is set to true
	 * Internal variables TBV: 	isClientManagerRunning
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);
		
		tempOutputStream = new ObjectOutputStream(mockTCPclientTest.getClientSocket().getOutputStream());
		tempInputStream = new ObjectInputStream(mockTCPclientTest.getClientSocket().getInputStream());
		
		clientManager_1 = new ClientManager(tempOutputStream, tempInputStream, sensor_ID_1);

		assertTrue(clientManager_1.isClientManagerRunning());
	}
	
   @After
   public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+ClientManagerTest.testID+" teardown section:");
	   
	   if(mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if (clientManager_1.isClientManagerRunning()) {
		   clientManager_1.getInputReaderStream().close();
		   clientManager_1.getOutputStream().close();
	   }
	   
	   if(Local_1h_Watchdog.getInstance() != null) {
		   Local_1h_Watchdog.getInstance().setM_instance(null);
	   }
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
   }

}
