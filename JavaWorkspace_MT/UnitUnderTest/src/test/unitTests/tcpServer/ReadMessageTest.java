package tcpServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.ObjectInputStream;
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
import messages.ClientMessage_ACK;
import messages.Message_Interface;
import tcpClient.ClientManager;
import tcpClient.TCPclient;

public class ReadMessageTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	final String serverHostName = "localhost";
	double global_watchdog_scale_factor = 0.01;
	ComputeEngine_Runnable comp_engine_1 = null;
	Thread testThread_readMessages = null;
	Thread testThread_exception = null;
	
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
	ObjectInputStream obj_in_stream = null;
	
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
	
	// to mock TCPclient class instance
	TCPclient mockTCPclient = null;
		
	// to mock server threads
	Thread mockServerThread = null;
	
	String[] testPurpose = { "Verify that once the readMessage() function is called, an incoming message from TCPclient is read from an input object stream",
							 "Verify that once the readMessage() function is called, it hangs in the getInputReaderStream().readObject() function until it receives a new message from TCPclient",
							 "Verify that SocketException is thrown if the server socket for an input object stream was closed while the readMessage() function was expecting for a new message from TCPclient",
			 				 "Verify that ClassCastException is thrown if the message received by the readMessage() function is not a message of the Message_Interface type"};
	static int testID = 1;
	
	public static void incrementTestID() {
		ReadMessageTest.testID += 1;
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
		                			System.out.println("mockTCPserver Thread Started.");
		                			comp_engine_1 = new ComputeEngine_Runnable(mock_CER_ClientSocket, global_watchdog_scale_factor, true);
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
		
		if(ReadMessageTest.testID < 3) {
			// test thread that listens for messages on the server side
			testThread_readMessages = new Thread(new Runnable() {
				//Runnable serverTask = new Runnable() {
				public void run() {
					try {
						while(comp_engine_1.get_ComputeEngineRunning()) {
							receivedMessage = (Message_Interface) comp_engine_1.readMessage(comp_engine_1.getInputReaderStream());
						}
					} catch (ClassNotFoundException e) {
						// To prove that exception's stack trace reported by JUnit caught ClassNotFoundException
						assertTrue(false);
						e.printStackTrace();
					} catch (IOException e) {
						// To prove that exception's stack trace reported by JUnit caught IOException
						assertTrue(false);
						e.printStackTrace();
					}
				}
			});
		}
		
		System.out.println("\t\tTest Run "+ReadMessageTest.testID+" Purpose:");
		System.out.println(testPurpose[(ReadMessageTest.testID-1)]);
		System.out.println("\t\tTest Run "+ReadMessageTest.testID+" Logic:");
	}
	
   /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that once the readMessage() function is called, an incoming message from TCPclient is read from an input object stream
	 * Internal variables TBV:		inputStream
	 * External variables TBV:		ClientMessage_ACK, Message_Interface
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		// create ObjectInputStream on the client to once a ComputeEngine_Runnable class instance is created
		obj_in_stream = new ObjectInputStream(mockTCPclient.getClientSocket().getInputStream());
		when(mockClientManager.getInputReaderStream()).thenReturn(obj_in_stream);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		obj_out_stream.writeObject(new ClientMessage_ACK(sensor_ID_1));
		Thread.sleep(20);
		
		assertTrue(receivedMessage instanceof ClientMessage_ACK);
	}
	
	/***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that once the readMessage() function is called, it hangs in the getInputReaderStream().readObject() function until it receives a new message from TCPclient
	 * Internal variables TBV:		inputStream
	 * External variables TBV:		ClientMessage_ACK, Message_Interface
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer(), ClientManager.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		// create ObjectInputStream on the client to once a ComputeEngine_Runnable class instance is created
		obj_in_stream = new ObjectInputStream(mockTCPclient.getClientSocket().getInputStream());
		when(mockClientManager.getInputReaderStream()).thenReturn(obj_in_stream);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		Thread.sleep(20);
		
		Message_Interface receivedMessage_old = null;
		Message_Interface receivedMessage_new = null;
			
		assertEquals(null, 							receivedMessage);
		
		obj_out_stream.writeObject(new ClientMessage_ACK(sensor_ID_1));
		Thread.sleep(20);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_old = receivedMessage;
		
		obj_out_stream.writeObject(new ClientMessage_ACK(sensor_ID_1));
		Thread.sleep(20);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_new = receivedMessage;
		
		assertNotEquals(receivedMessage_new, 		receivedMessage_old);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that SocketException is thrown if the server socket for an input object stream was closed while
	 								the readMessage() function was expecting for a new message from TCPclient
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer()
	 * Exceptions thrown TBV:		SocketException
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		// create ObjectInputStream on the client to once a ComputeEngine_Runnable class instance is created
		obj_in_stream = new ObjectInputStream(mockTCPclient.getClientSocket().getInputStream());
		when(mockClientManager.getInputReaderStream()).thenReturn(obj_in_stream);
		
		final String expected_exception_name = "java.net.SocketException";
		
		// test thread that listens for exceptions caused on the client side
		testThread_exception = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					receivedMessage = (Message_Interface) comp_engine_1.readMessage(comp_engine_1.getInputReaderStream());
				} catch (ClassNotFoundException e) {
					// To prove that exception's stack trace reported by JUnit caught ClassNotFoundException
					assertTrue(false);
					e.printStackTrace();
				} catch (SocketException e) {
					testThread_exception.interrupt();
					// To prove that exception's stack trace reported by JUnit caught SocketException
					assertTrue(testThread_exception.isInterrupted());
	        		assertEquals(expected_exception_name,		e.getClass().getName());
				} catch (IOException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					assertTrue(false);
					e.printStackTrace();
				}
			}
		});
		testThread_exception.start();
		Thread.sleep(20);
		
		mockTCPserverTest.getServerSocket().close();
		Thread.sleep(20);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that ClassCastException is thrown if the message received by the readMessage() function is not a message of the Message_Interface type
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPclient, TCPserver, ClientManager, Socket
	 * Mocks methods called:		TCPserver.startServer()
	 * Exceptions thrown TBV:		ClassCastException
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_4() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		tempClientSocket_1 = new Socket(serverHostName, port_1);
		when(mockTCPclient.getClientSocket()).thenReturn(tempClientSocket_1);
		
		// create ObjectOutputStream on the client side to activate mock_CER_ClientSocket = servSocket.accept() in mockServerThread
		obj_out_stream = new ObjectOutputStream(mockTCPclient.getClientSocket().getOutputStream());
		when(mockClientManager.getOutputStream()).thenReturn(obj_out_stream);
		
		// create ObjectInputStream on the client to once a ComputeEngine_Runnable class instance is created
		obj_in_stream = new ObjectInputStream(mockTCPclient.getClientSocket().getInputStream());
		when(mockClientManager.getInputReaderStream()).thenReturn(obj_in_stream);
		
		final String expected_exception_name = "java.lang.ClassCastException";
		
		// test thread that listens for exceptions caused on the client side
		testThread_exception = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					receivedMessage = (Message_Interface) comp_engine_1.readMessage(comp_engine_1.getInputReaderStream());
				}
				catch (ClassCastException e) {
					testThread_exception.interrupt();
					// To prove that exception's stack trace reported by JUnit caught SocketException
					assertTrue(testThread_exception.isInterrupted());
	        		assertEquals(expected_exception_name,		e.getClass().getName());
				} catch (ClassNotFoundException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					assertTrue(false);
					e.printStackTrace();
				} catch (IOException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					assertTrue(false);
					e.printStackTrace();
				}
			}
		});
		testThread_exception.start();
		Thread.sleep(20);
		
		obj_out_stream.writeObject(new String(" "));
		Thread.sleep(20);
	}
		
	
	@After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+ReadMessageTest.testID+" teardown section:");
	   
	   if(mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if (mockClientManager.getInputReaderStream() != null) {
		   mockClientManager.closeInStream();
	   }
	   if (mockClientManager.getOutputStream() != null){
		   mockClientManager.closeOutStream();
	   }
	   if(testThread_readMessages != null) {
		   if (testThread_readMessages.isAlive()) {
			   testThread_readMessages.interrupt();
		   }
	   }
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
}
