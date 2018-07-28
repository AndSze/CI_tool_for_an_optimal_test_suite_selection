package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import messages.Message_Interface;
import tcpClient.ClientManager;
import tcpServer.Delays;
import tcpClient.TCPclient;

public class Set_delays_arrayTest {
	
	int port_1 = 9876;
	int sensor_ID_1 = 1;
	final String serverHostName = "localhost";
	double global_watchdog_scale_factor = 1.0;
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
    private final static int delays_array_size = 4;
	
	String[] testPurpose = { "Verify default delays values in delays_array. This array has default values if watchdog_scale_factor equals 1",
							 "Verify that delays values in delays_array are scaled based on watchdog_scale_factor if it does not equal 1"};

	static int testID = 1;
	
	public static void incrementTestID() {
		Set_delays_arrayTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
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
		
		System.out.println("\t\tTest Run "+Set_delays_arrayTest.testID+" Purpose:");
		System.out.println(testPurpose[(Set_delays_arrayTest.testID-1)]);
		System.out.println("\t\tTest Run "+Set_delays_arrayTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify default delays values in delays_array. This array has default values if watchdog_scale_factor equals 1
	 * Internal variables TBV: 	delays_array
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		
		int temp_delays_array_0 = 100;
		int temp_delays_array_1 = 1000;
		int temp_delays_array_2 = 10000;
		int temp_delays_array_3 = 100000;
		
		assertEquals(temp_delays_array_0,    			comp_engine_1.get_delays(Delays.LOWEST, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_1,    			comp_engine_1.get_delays(Delays.LOW, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_2,    			comp_engine_1.get_delays(Delays.MEDIUM, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_3,    			comp_engine_1.get_delays(Delays.HIGHEST, comp_engine_1.delays_array));

	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify delays values in delays_array that are scaled based on watchdog_scale_factor if it does not equal 1
	 * Internal variables TBV: 	delays_array
     * Exceptions thrown: 		IOException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException {
		
		double temp_watchdog_scale_factor_1 = 0.01;
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, temp_watchdog_scale_factor_1, true);
		
		int temp_delays_array_01 = (int) (100 * temp_watchdog_scale_factor_1);
		int temp_delays_array_11 = (int) (1000 * temp_watchdog_scale_factor_1);
		int temp_delays_array_21 = (int) (10000 * temp_watchdog_scale_factor_1);
		int temp_delays_array_31 = (int) (100000 * temp_watchdog_scale_factor_1);
		
		assertEquals(temp_delays_array_01,    			comp_engine_1.get_delays(Delays.LOWEST, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_11,    			comp_engine_1.get_delays(Delays.LOW, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_21,    			comp_engine_1.get_delays(Delays.MEDIUM, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_31,    			comp_engine_1.get_delays(Delays.HIGHEST, comp_engine_1.delays_array));
		
		double temp_watchdog_scale_factor_2 = 0.25;
		
		comp_engine_1.delays_array = comp_engine_1.set_delays_array(temp_watchdog_scale_factor_2, delays_array_size);
		
		int temp_delays_array_02 = (int) (100 * temp_watchdog_scale_factor_2);
		int temp_delays_array_12 = (int) (1000 * temp_watchdog_scale_factor_2);
		int temp_delays_array_22 = (int) (10000 * temp_watchdog_scale_factor_2);
		int temp_delays_array_32 = (int) (100000 * temp_watchdog_scale_factor_2);
		
		assertEquals(temp_delays_array_02,    			comp_engine_1.get_delays(Delays.LOWEST, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_12,    			comp_engine_1.get_delays(Delays.LOW, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_22,    			comp_engine_1.get_delays(Delays.MEDIUM, comp_engine_1.delays_array));
		assertEquals(temp_delays_array_32,    			comp_engine_1.get_delays(Delays.HIGHEST, comp_engine_1.delays_array));

	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+Set_delays_arrayTest.testID+" teardown section:");
	   
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

