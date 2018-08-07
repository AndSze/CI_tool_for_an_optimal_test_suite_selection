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
import sensor.SensorImpl;
import tcpClient.ClientManager;
import tcpClient.TCPclient;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class _1h_Watchdog_close_to_expireTest {

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
	
	String[] testPurpose = { "Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.MEDIUM and no_of_measurements different than TCPserver.getMeasurements_limit() - 1",
							 "Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.LOWEST and no_of_measurements different than TCPserver.getMeasurements_limit() - 1",
							 "Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog lower than Watchdog_Thresholds.LOWEST and no_of_measurements different than TCPserver.getMeasurements_limit() - 1",
							 "Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.MEDIUM and no_of_measurements equal to TCPserver.getMeasurements_limit() - 1",
							 "Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.LOWEST and no_of_measurements equal to TCPserver.getMeasurements_limit() - 1",
	 						 "Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog lower than Watchdog_Thresholds.LOWEST and no_of_measurements equal to TCPserver.getMeasurements_limit() - 1"};

	static int testID = 1;
	
	public static void incrementTestID() {
		_1h_Watchdog_close_to_expireTest.testID += 1;
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
		
		System.out.println("\t\tTest Run "+_1h_Watchdog_close_to_expireTest.testID+" Purpose:");
		System.out.println(testPurpose[(_1h_Watchdog_close_to_expireTest.testID-1)]);
		System.out.println("\t\tTest Run "+_1h_Watchdog_close_to_expireTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.MEDIUM
	 							and no_of_measurements different than TCPserver.getMeasurements_limit() - 1
	 * Internal variables TBV: 	local_1h_watchdog
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		IOException, InterruptedException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException  {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		comp_engine_1.setSensor(new SensorImpl(sensor_ID_1));
		
		double input_1h_watchdog = 200 * global_watchdog_scale_factor;
		double expected_1h_watchdog_readout = input_1h_watchdog * 0.25;
		double expected_1h_Global_watchdog_readout = input_1h_watchdog * 0.5;
		double expected_24h_Global_watchdog_readout = input_1h_watchdog * 0.5;
		int no_of_measurements = 22;
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double output_1h_watchdog_readout = 0.0;
		output_1h_watchdog_readout = comp_engine_1._1h_Watchdog_close_to_expire(input_1h_watchdog, global_watchdog_scale_factor, no_of_measurements);
		
		assertEquals(expected_1h_watchdog_readout, 				output_1h_watchdog_readout, 0.1);
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.2);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.2);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.LOWEST
	 							and no_of_measurements different than TCPserver.getMeasurements_limit() - 1
	 * Internal variables TBV: 	local_1h_watchdog
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		IOException, InterruptedException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException  {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		comp_engine_1.setSensor(new SensorImpl(sensor_ID_1));
		
		double input_1h_watchdog = 120 * global_watchdog_scale_factor;
		double expected_1h_watchdog_readout = input_1h_watchdog * 0.75;
		double expected_1h_Global_watchdog_readout = input_1h_watchdog * 0.75;
		double expected_24h_Global_watchdog_readout = input_1h_watchdog * 0.75;
		int no_of_measurements = 22;
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double output_1h_watchdog_readout = 0.0;
		output_1h_watchdog_readout = comp_engine_1._1h_Watchdog_close_to_expire(input_1h_watchdog, global_watchdog_scale_factor, no_of_measurements);
		
		assertEquals(expected_1h_watchdog_readout, 				output_1h_watchdog_readout, 0.1);
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog lower than Watchdog_Thresholds.LOWEST
	 							and no_of_measurements different than TCPserver.getMeasurements_limit() - 1
	 * Internal variables TBV: 	local_1h_watchdog
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		IOException, InterruptedException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException  {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		comp_engine_1.setSensor(new SensorImpl(sensor_ID_1));
		
		double input_1h_watchdog = 80 * global_watchdog_scale_factor;
		double expected_1h_watchdog_readout = input_1h_watchdog * 0.5;
		double expected_1h_Global_watchdog_readout = input_1h_watchdog * 1.0;
		double expected_24h_Global_watchdog_readout = input_1h_watchdog * 1.0;
		int no_of_measurements = 22;
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double output_1h_watchdog_readout = 0.0;
		output_1h_watchdog_readout = comp_engine_1._1h_Watchdog_close_to_expire(input_1h_watchdog, global_watchdog_scale_factor, no_of_measurements);
		
		assertEquals(expected_1h_watchdog_readout, 				output_1h_watchdog_readout, 0.1);
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.MEDIUM
	 							and no_of_measurements equal to TCPserver.getMeasurements_limit() - 1
	 * Internal variables TBV: 	local_1h_watchdog
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		IOException, InterruptedException 
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException  {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		comp_engine_1.setSensor(new SensorImpl(sensor_ID_1));
		
		double input_1h_watchdog = 200 * global_watchdog_scale_factor;
		double expected_1h_watchdog_readout = input_1h_watchdog * 0.25;
		double expected_1h_Global_watchdog_readout = input_1h_watchdog * 0.5;
		double expected_24h_Global_watchdog_readout = input_1h_watchdog * 1;
		int no_of_measurements = TCPserver.getMeasurements_limit() - 1;
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double output_1h_watchdog_readout = 0.0;
		output_1h_watchdog_readout = comp_engine_1._1h_Watchdog_close_to_expire(input_1h_watchdog, global_watchdog_scale_factor, no_of_measurements);
		
		assertEquals(expected_1h_watchdog_readout, 				output_1h_watchdog_readout, 0.1);
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog higher than Watchdog_Thresholds.LOWEST
	 							and no_of_measurements equal to TCPserver.getMeasurements_limit() - 1
	 * Internal variables TBV: 	local_1h_watchdog
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_5() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		comp_engine_1.setSensor(new SensorImpl(sensor_ID_1));
		
		double input_1h_watchdog = 120 * global_watchdog_scale_factor;
		double expected_1h_watchdog_readout = input_1h_watchdog * 0.5;
		double expected_1h_Global_watchdog_readout = input_1h_watchdog * 1;
		double expected_24h_Global_watchdog_readout = input_1h_watchdog * 1;
		int no_of_measurements = TCPserver.getMeasurements_limit() - 1;
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double output_1h_watchdog_readout = 0.0;
		output_1h_watchdog_readout = comp_engine_1._1h_Watchdog_close_to_expire(input_1h_watchdog, global_watchdog_scale_factor, no_of_measurements);
		
		assertEquals(expected_1h_watchdog_readout, 				output_1h_watchdog_readout, 0.1);
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_6
	 * Description: 			Verify outputs of the _1h_Watchdog_close_to_expire() function call for input_1h_watchdog lower than Watchdog_Thresholds.LOWEST
	 							and no_of_measurements equal to TCPserver.getMeasurements_limit() - 1
	 * Internal variables TBV: 	local_1h_watchdog
	 * External variables TBV: 	Global_24h_Watchdog.millisecondsLeftUntilExpiration, Global_1h_Watchdog.millisecondsLeftUntilExpiration
     * Exceptions thrown: 		IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_6() throws IOException, InterruptedException  {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		Thread.sleep(20);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
		comp_engine_1.setSensor(new SensorImpl(sensor_ID_1));
		
		double input_1h_watchdog = 80 * global_watchdog_scale_factor;
		double expected_1h_watchdog_readout = input_1h_watchdog * 0.5;
		double expected_1h_Global_watchdog_readout = input_1h_watchdog * 1;
		double expected_24h_Global_watchdog_readout = input_1h_watchdog * 1;
		int no_of_measurements = 23;
		
		Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(input_1h_watchdog);
		Global_1h_Watchdog.getInstance().setEnabled(true);
		Global_24h_Watchdog.getInstance().setEnabled(true);
		
		double output_1h_watchdog_readout = 0.0;
		output_1h_watchdog_readout = comp_engine_1._1h_Watchdog_close_to_expire(input_1h_watchdog, global_watchdog_scale_factor, no_of_measurements);
		
		assertEquals(expected_1h_watchdog_readout, 				output_1h_watchdog_readout, 0.1);
		assertEquals(expected_1h_Global_watchdog_readout, 		Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
		assertEquals(expected_24h_Global_watchdog_readout, 		Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 0.1);
	}

	@After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+_1h_Watchdog_close_to_expireTest.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(mockTCPserverTest);
	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
		   
}
