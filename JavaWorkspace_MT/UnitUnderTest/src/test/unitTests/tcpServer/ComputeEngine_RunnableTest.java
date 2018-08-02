package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import messages.Message_Interface;
import tcpClient.ClientManager;
import tcpClient.TCPclient;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;



public class ComputeEngine_RunnableTest {
	
	int port_1 = 9876;
	int sensor_ID_1 = 1;
	final String serverHostName = "localhost";
	double global_watchdog_scale_factor = 0.01;
	ComputeEngine_Runnable comp_engine_1 = null;
	
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	
	// Socket that are real object used for the TCP connection purposes
	ServerSocket tempServerSocket_1 = null;
	Socket tempClientSocket_1 = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement, this socket is used in ComputeEngine_Runnable class instance
	Socket mock_CER_ClientSocket = null;
	
	// to mock Client Manager class instance
	ClientManager mockClientManager = null;
	ObjectOutputStream obj_out_stream = null;
	
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
	
	// to mock TCPclient class instance
	TCPclient mockTCPclient = null;
		
	// to mock server threads
	Thread mockServerThread = null;

	String[] testPurpose = { "Verify that once the default constructor of the ComputeEngine_Runnable class is called, outputStream and inputStream object streams are created for the purpose of setting up the TCP connection set TCPclient based on the client's socket that is 1st argument in the constructor call",
							 "Verify that once the default constructor of the ComputeEngine_Runnable class is called, local_watchdog_scale_factor is updated in accordance with global_watchdog_scale_factor",
							 "Verify that once the default constructor of the ComputeEngine_Runnable class is called, the isComputeEngine_Runnable_running flag is updated in accordance with 3rd input argument in the constructor call",
							 "Verify that once the default constructor of the ComputeEngine_Runnable class is called, time left before expiration for Local_1h_watchdog and Local_24h_watchdog is updated accordingly to the time left before expiration for Global_1h_watchdog and Global_24h_watchdog",
							 "Verify that once the default constructor of the ComputeEngine_Runnable class is called, delays_array and watchdog_thresholds_array are updated in accordance with global_watchdog_scale_factor",
							 "Verify that the Socket Exception is returned if there was an attempt to create a ComputeEngine_Runnable class instance with a socket that is closed"};
	static int testID = 1;
	
	public static void incrementTestID() {
		ComputeEngine_RunnableTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException, ClassNotFoundException {
		
		// mocked objects 
		mockTCPserverTest = mock(TCPserver.class);
		mock_CER_ClientSocket = mock(Socket.class);
		mockClientManager = mock(ClientManager.class);
		mockTCPclient = mock(TCPclient.class);
		
		// create a real Server Socket for the TCPserver mock to enable the Client Socket to set up the TCP connection
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
		                			mock_CER_ClientSocket = servSocket.accept();
		                			System.out.println("Server Thread Started.");
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
		
		System.out.println("\t\tTest Run "+ComputeEngine_RunnableTest.testID+" Purpose:");
		System.out.println(testPurpose[(ComputeEngine_RunnableTest.testID-1)]);
		System.out.println("\t\tTest Run "+ComputeEngine_RunnableTest.testID+" Logic:");
	}
	

    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the default constructor of the ComputeEngine_Runnable class is called, outputStream and inputStream object streams are created 
	 							for the purpose of setting up the TCP connection set TCPclient based on the client's socket that is 1st argument in the constructor call
	 * Internal variables TBV: 	outputStream, inputStream
	 * Mocked objects:			TCPserver, TCPclient, ClientManager, Socket
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		assertNotEquals(null,		 comp_engine_1.getInputReaderStream());
		assertNotEquals(null, 		 comp_engine_1.getOutputStream());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the default constructor of the ComputeEngine_Runnable class is called, local_watchdog_scale_factor is updated in accordance with global_watchdog_scale_factor
	 * Internal variables TBV: 	local_watchdog_scale_factor
	 * Mocked objects:			TCPserver, TCPclient, ClientManager, Socket
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		assertEquals(global_watchdog_scale_factor, 					comp_engine_1.getLocal_watchdog_scale_factor(), 0.001);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the default constructor of the ComputeEngine_Runnable class is called, the isComputeEngine_Runnable_running flag is updated in accordance with 
	 							3rd input argument in the constructor call
	 * Internal variables TBV: 	isComputeEngine_Runnable_running
	 * Mocked objects:			TCPserver, TCPclient, ClientManager, Socket
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, false);
		
		assertFalse(comp_engine_1.isComputeEngine_Runnable_running());
		
		comp_engine_1.closeInStream();
		comp_engine_1.closeOutStream();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		assertTrue(comp_engine_1.isComputeEngine_Runnable_running());

	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that once the default constructor of the ComputeEngine_Runnable class is called, time left before expiration for Local_1h_watchdog and Local_24h_watchdog
	  							is updated accordingly to the time left before expiration for Global_1h_watchdog and Global_24h_watchdog
	 * Internal variables TBV: 	local_1h_watchdog, local_24h_watchdog
	 * Mocked objects:			TCPserver, TCPclient, ClientManager, Socket
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		assertEquals(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 			comp_engine_1.getLocal_1h_watchdog(), 0.1);
		assertEquals(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 			comp_engine_1.getLocal_24h_watchdog(), 0.1);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that once the default constructor of the ComputeEngine_Runnable class is called, delays_array and watchdog_thresholds_array are updated in accordance with global_watchdog_scale_factor
	 * Internal variables TBV: 	delays_array, watchdog_thresholds_array
	 * Mocked objects:			TCPserver, TCPclient, ClientManager, Socket
     * Mocked external methods: TCPserver.startServer()
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
	    int[] temp_delays_array = null;
	    final int delays_array_size = 4;
	    double[] temp_watchdog_thresholds_array = null;
	    final int watchdog_thresholds_array_size = 4;
	    
	    temp_delays_array = comp_engine_1.set_delays_array(global_watchdog_scale_factor, delays_array_size);
	    temp_watchdog_thresholds_array = comp_engine_1.set_watchdog_thresholds_array(global_watchdog_scale_factor, watchdog_thresholds_array_size);
		
	    for (int i = 0; i < delays_array_size; i ++) {
	    	assertEquals(temp_delays_array[i],							comp_engine_1.getDelays_array()[i]);
	    }
		
		for (int i = 0; i < watchdog_thresholds_array_size; i ++) {
			 assertEquals(temp_watchdog_thresholds_array[i],			comp_engine_1.getWatchdog_thresholds_array()[i], 0.001);	
		}		
	}
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify that the Socket Exception is returned if there was an attempt to create a ComputeEngine_Runnable class instance with a socket that is closed
	 * Mocked external methods: TCPserver.startServer()
	 * Mocked objects:			TCPserver, TCPclient, ClientManager, Socket
	 * Exceptions thrown TBV:	SocketException
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_6() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		mock_CER_ClientSocket.close();
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
	
		// To prove that exception's stack trace reported by JUnit caught the SocketException
		assertTrue(false);
	}
	
	@After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+ComputeEngine_RunnableTest.testID+" teardown section:");
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   if (mockClientManager.getOutputStream() != null){
		   mockClientManager.closeOutStream();
	   }
	   if (mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   
	   System.out.println("");
	   incrementTestID();
    }
		   
}
