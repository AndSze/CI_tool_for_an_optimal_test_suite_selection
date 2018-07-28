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

public class CloseInStreamTest {
	
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

	String[] testPurpose = { "Verify that once the closeInStream() function for a ComputeEngine_Runnable class instance is called, the object input stream is closed - it is verified by SocketException that is thrown when there was an attempt to read an object from the previously closed object input stream"};
	static int testID = 1;
	
	public static void incrementTestID() {
		CloseInStreamTest.testID += 1;
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
		
		System.out.println("\t\tTest Run "+CloseInStreamTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseInStreamTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseInStreamTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that once the closeInStream() function for a ComputeEngine_Runnable class instance is called, the object input stream is closed
	  								- it is verified by SocketException that is thrown when there was an attempt to read an object from the previously closed object input stream
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPserver, TCPclient, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer()
	 * Exceptions thrown TBV:		SocketException
     * Exceptions thrown: 			IOException, ClassNotFoundException, InterruptedException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_1() throws IOException, ClassNotFoundException, InterruptedException {
	
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, false);
		Thread.sleep(20);
				
		comp_engine_1.closeInStream();
		
		receivedMessage = (Message_Interface) (comp_engine_1.readMessage(comp_engine_1.getInputReaderStream()));
		
		// To prove that exception's stack trace reported by JUnit caught SocketException
		assertTrue(false);
	}
	
	@After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+CloseInStreamTest.testID+" teardown section:");
	   
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
