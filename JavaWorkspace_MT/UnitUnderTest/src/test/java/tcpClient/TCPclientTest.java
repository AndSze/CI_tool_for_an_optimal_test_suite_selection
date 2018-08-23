package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ConnectException;
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
import messages.SensorState;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;

public class TCPclientTest {
	
	int port_1 = 9876;
	int sensor_ID_1 = 1;
	TCPclient tcpclient_1 = null;
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
	
	String[] testPurpose = { "Verify that the Connect Exception is returned if there was an attempt to call the overloaded constructor of the TCPclient without a server instance initialized previously",
							 "Verify that once the overloaded constructor of the TCPclient class is being called, the client socket is created at specific port and on given server host",
							 "Verify that once the overloaded constructor of the TCPclient class is being called, the client manager for the TCPclient class instance is initialized with input and output streams",
							 "Verify that once the overloaded constructor of the TCPclient class is being called, the clientRunning flag is set to true",
							 "Verify that once the overloaded constructor of the TCPclient class is being called with a unique senor ID , Client_Sensors_LIST is updated with the newly created SensorImpl class instance with specified sensorID",
							 "Verify that once the overloaded constructor of the TCPclient class is being called with a senor ID that already exists for a sensor in Client_Sensors_LIST, the list is not being updated and no SensorImpl class instance is created"};
							 
	static int testID = 1;
	
	public static void incrementTestID() {
		TCPclientTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		// to initialize Client_Sensors_LIST
		new TCPclient();
		
		if(TCPclientTest.testID > 1 ) {
			// mock Server Socket to enable the Client Socket to establish connection
			mockTCPserverTest = mock(TCPserver.class);
			mockClientSocket = mock(Socket.class);
			
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
		// the test uses this approach for the purpose of avoiding actual messages sent via TCP - it will be checked in the integration tests
		}
		
		System.out.println("\t\tTest Run "+TCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(TCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+TCPclientTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that the Connect Exception is returned if there was an attempt to create the client without a server instance initialized previously
	 * Exceptions thrown TBV:	ConnectException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = ConnectException.class)
	public void test_run_1() throws IOException {
		
		tcpclient_1 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called for the first time, Client_Sensors_LIST is created
	 * Internal variables TBV: 	clientSocket
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
				
		tcpclient_1 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		assertNotEquals(null, 		tcpclient_1.getClientSocket());
		assertFalse(tcpclient_1.getClientSocket().isClosed());
		assertTrue(tcpclient_1.getClientSocket().isBound());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called for the first time, Client_Sensors_LIST is created
	 * Internal variables TBV: 	clientManager
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		assertNotEquals(null, 		tcpclient_1.getClientManager());
		assertNotEquals(null, 		tcpclient_1.getClientManager().getInputReaderStream());
		assertNotEquals(null, 		tcpclient_1.getClientManager().getOutputStream());
		assertTrue(tcpclient_1.getClientManager().isClientManagerRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called for the first time, Client_Sensors_LIST is created
	 * Internal variables TBV: 	clientRunning
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.isClientRunning());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that once the default constructor of the TCPclient class is being called for the first time, Client_Sensors_LIST is created
	 * Internal variables TBV: 	Client_Sensors_LIST
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_5() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		assertEquals(null,  						tcpclient_1.searchInClientSensorList(sensor_ID_1));

		tcpclient_1 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		assertNotEquals(null,  						tcpclient_1.searchInClientSensorList(sensor_ID_1));
		assertEquals(sensor_ID_1,  					tcpclient_1.searchInClientSensorList(sensor_ID_1).getSensorID());
		assertEquals(SensorState.PRE_OPERATIONAL,  	tcpclient_1.searchInClientSensorList(sensor_ID_1).getSensorState());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that once the overloaded constructor of the TCPclient class is being called with a senor ID that already
	  							exists for a sensor in Client_Sensors_LIST, the list is not being updated and no SensorImpl class instance is created
	 * Internal variables TBV: 	Client_Sensors_LIST
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_6() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tcpclient_1 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		int sensor_list_size_1 = tcpclient_1.Client_Sensors_LIST.size();
		
		tcpclient_2 = new TCPclient(sensor_ID_1, serverHostName, port_1);
		
		int sensor_list_size_2 = tcpclient_2.Client_Sensors_LIST.size();
		
		assertEquals(sensor_list_size_1,  				sensor_list_size_2);
		assertEquals(tcpclient_1.Client_Sensors_LIST,  	tcpclient_2.Client_Sensors_LIST);
	}

   @SuppressWarnings("static-access")
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+TCPclientTest.testID+" teardown section:");
	   
	   if(tcpclient_1.searchInClientSensorList(sensor_ID_1) != null) {
		   tcpclient_1.Client_Sensors_LIST.remove(tcpclient_1.searchInClientSensorList(sensor_ID_1));
	   }
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
