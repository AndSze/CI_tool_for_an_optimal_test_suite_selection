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
import messages.Message_Interface;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;

public class CloseInStreamTest {

	ClientManager clientManager_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    String serverHostName  = "localhost";
    Thread testThread = null;
    
    // Client Socket for the TCPclient class mock
	Socket TCPclientSocket = null;
	
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
    
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
	
	String[] testPurpose = { "Verify that once the closeInStream() function for a client manager class instance is called, the object input stream is closed - it is verified by SocketException that is thrown when there was an attempt to read an object from the previously closed object input stream",
							 "Verify that IllegalArgumentException is thrown if there was an attempt to call the closeInStream() function for a client manager class instance without initializing its object input stream"};

	static int testID = 1;
	
	public static void incrementTestID() {
		CloseInStreamTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		// call the default constructor for the ClientManager class to create an instance: clientManager_1
		clientManager_1 = new ClientManager();
		
		// mocked objects 
		mockTCPserverTest = mock(TCPserver.class);
		mockTCPclientTest = mock(TCPclient.class);
		mockClientSocket = mock(Socket.class);
		mockComputeEngine_Runnable = mock(ComputeEngine_Runnable.class);
		
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
		
		System.out.println("\t\tTest Run "+CloseInStreamTest.testID+" Purpose:");
		System.out.println(testPurpose[(CloseInStreamTest.testID-1)]);
		System.out.println("\t\tTest Run "+CloseInStreamTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that once the closeInStream() function for a client manager class instance is called, the object input stream is closed
	  								- it is verified by SocketException that is thrown when there was an attempt to read an object from the previously closed object input stream
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPserver, TCPclient, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer()
	 * Exceptions thrown TBV:		SocketException
     * Exceptions thrown: 			IOException, ClassNotFoundException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_1() throws IOException, ClassNotFoundException {
	
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
				
		clientManager_1.closeInStream();
		
		receivedMessage = (Message_Interface) (clientManager_1.readMessage(clientManager_1.getInputReaderStream()));
		
		// To prove that exception's stack trace reported by JUnit caught SocketException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that IllegalArgumentException is thrown if there was an attempt to
	  								call the closeInStream() function for a client manager class instance without initializing its object input stream
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPserver, TCPclient, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer()
	 * Exceptions thrown TBV:		IllegalArgumentException
     * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	@Test(expected = IllegalArgumentException.class)
	public void test_run_2() throws IOException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1.closeInStream();
		
		// To prove that exception's stack trace reported by JUnit caught IllegalArgumentException
		assertTrue(false);
	}
	
   @After
   public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+CloseInStreamTest.testID+" teardown section:");
	   
	   if ( (mockTCPserverTest.getServerSocket().isBound()) && (!mockTCPserverTest.getServerSocket().isClosed()) ){
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if (clientManager_1.getInputReaderStream() != null) {
		   clientManager_1.closeOutStream();
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}

}
