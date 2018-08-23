package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
import messages.ClientMessage_BootUp;
import messages.Message_Interface;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;

public class CloseClientManagerTest {
	
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
	
	String[] testPurpose = { 	"Verify that once the closeClientManger() function is called, the input object stream for the TCPclient class instance is closed - it is verified by proving that SocketException is thrown",
								"Verify that once the closeClientManger() function is called, the output object stream for the TCPclient class instance is closed - it is verified by proving that SocketException is thrown",
								"Verify that once the closeClientManger() function is called, the flag that indicates if a client manager is running is being set to false",
								"Verify that once the closeClientManger() function is called, the flag that indicates if a client is running is not being set to false",
								"Verify that once the closeClientManger() function is called for a client instance at some port, the other client managers are not being closed",
							    "Verify that there is IllegalArgumentException thrown if there was an attempt to call the closeClientManger() function for the TCPclient class instance without initializing a client manager previously"};
	static int testID = 1;
	
	public static void incrementTestID() {
		CloseClientManagerTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpclient_1 = new TCPclient();
		tcpclient_2 = new TCPclient();

		
		// mock Server Socket to enable the Client Socket to establish connection
		mockTCPserverTest = mock(TCPserver.class);
		mockClientSocket = mock(Socket.class);
		
		tempServerSocket_1 = new ServerSocket();
		when(mockTCPserverTest.getServerSocket()).thenReturn(tempServerSocket_1);
		
		
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

		System.out.println("\t\tTest Run "+CloseClientManagerTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseClientManagerTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseClientManagerTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the closeClientManger() function is called, the input object stream for the TCPclient class instance is closed 
	 							- it is verified by proving that SocketException is thrown
	 * External variables TBV: 	ClientManager.inputStream
	 * Mocked external methods: TCPserver.startServer()
	 * Exceptions thrown TBV:	SocketException
     * Exceptions thrown: 		IOException, ClassNotFoundException
	 ***********************************************************************************************************/
	@SuppressWarnings("unused")
	@Test(expected = SocketException.class)
	public void test_run_1() throws IOException, ClassNotFoundException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		Message_Interface receivedMessage = null;
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.getClientManager().isClientManagerRunning());

		tcpclient_1.closeClientManager(tcpclient_1);
		
		receivedMessage = (Message_Interface) tcpclient_1.getClientManager().getInputReaderStream().readObject();
		
		// To prove that exception's stack trace reported by JUnit caught the SocketException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the closeClientManger() function is called, the output object stream for the TCPclient class instance is closed 
	 							- it is verified by proving that SocketException is thrown
	 * External variables TBV: 	ClientManager.outputStream
	 * Mocked external methods: TCPserver.startServer()
	 * Exceptions thrown TBV:	SocketException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_2() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.getClientManager().isClientManagerRunning());
		tcpclient_1.getClientManager().getOutputStream().writeObject(new ClientMessage_BootUp(sensor_ID_1));

		tcpclient_1.closeClientManager(tcpclient_1);
		
		tcpclient_1.getClientManager().getOutputStream().writeObject(new ClientMessage_BootUp(sensor_ID_1));
		
		// To prove that exception's stack trace reported by JUnit caught the SocketException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the closeClientManger() function is called, the flag that indicates if a client manager is running is being set to false
	 * External variables TBV: 	ClientManager.isClientManagerRunning
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.getClientManager().isClientManagerRunning());

		tcpclient_1.closeClientManager(tcpclient_1);
		
		assertFalse(tcpclient_1.getClientManager().isClientManagerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the closeClientManger() function is called, the flag that indicates if a client is running is not being set to false
	 * Internal variables TBV: 	clientRunning
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException {

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.getClientManager().isClientManagerRunning());
		assertTrue(tcpclient_1.isClientRunning());

		tcpclient_1.closeClientManager(tcpclient_1);
		
		assertTrue(tcpclient_1.isClientRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that once the closeClientManger() function is called for a client instance at some port, the other client managers are not being closed
	 * External variables TBV: 	ClientManager.isClientManagerRunning
	 * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException {

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_2, serverHostName, port_1);
		
		assertTrue(tcpclient_1.getClientManager().isClientManagerRunning());
		assertTrue(tcpclient_2.getClientManager().isClientManagerRunning());

		tcpclient_1.closeClientManager(tcpclient_1);
		
		assertFalse(tcpclient_1.getClientManager().isClientManagerRunning());
		assertTrue(tcpclient_2.getClientManager().isClientManagerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that there is IllegalArgumentException thrown if there was an attempt to call the closeClientManger() function
	  							for the TCPclient class instance without initializing a client manager previously
	 * Mocked external methods: TCPserver.startServer()
	 * Exceptions thrown TBV:	IllegalArgumentException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = IllegalArgumentException.class)
	public void test_run_6() throws IOException {

		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();

		tcpclient_1.closeClientManager(tcpclient_1);
		
		// To prove that exception's stack trace reported by JUnit caught the IllegalArgumentException
		assertTrue(false);
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{

		System.out.println("\t\tTest Run "+CloseClientManagerTest.testID+" teardown section:");

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
