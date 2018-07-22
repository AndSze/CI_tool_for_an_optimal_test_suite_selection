package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
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
import messages.ServerMessage_SensorInfoQuerry;
import sensor.SensorImpl;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;

public class ReadMessageTest {
	
	ClientManager clientManager_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    String serverHostName  = "localhost";
    Thread testThread_exception = null;
    Thread testThread_readMessages = null;
    SensorImpl sensor_1 = null;
    
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
	
	String[] testPurpose = { "Verify that once the readMessage() function is called, a message from TCPserver is being read from an input object stream",
							 "Verify that once the readMessage() function is called, it hangs in the getInputReaderStream().readObject() function until it receives a new message from TCPserver",
							 "Verify that SocketException is thrown if the client socket for an input object stream was closed while the readMessage() function was expecting for a new message from TCPserver",
			 				 "Verify that ClassCastException is thrown if the message received by the readMessage() function is not a message of the Message_Interface type"};
	static int testID = 1;
	
	public static void incrementTestID() {
		ReadMessageTest.testID += 1;
	}
	
	@SuppressWarnings("static-access")
	@Before
	public void before() throws IOException {
		
		// call the default constructor for the ClientManager class to create an instance: clientManager_1
		clientManager_1 = new ClientManager();
		
    	if (mockTCPclientTest.Client_Sensors_LIST == null) {
    		mockTCPclientTest.Client_Sensors_LIST = new ArrayList<>();
    	}
    	if (mockTCPclientTest.searchInClientSensorList(sensor_ID_1) == null) {
    		mockTCPclientTest.Client_Sensors_LIST = mockTCPclientTest.updateClientSensorList(new SensorImpl(sensor_ID_1));
    	}
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
		
		if (ReadMessageTest.testID < 3) {
			// test thread that listens for messages on the client side
			testThread_readMessages = new Thread(new Runnable() {
				//Runnable serverTask = new Runnable() {
				public void run() {
					try {
						while(clientManager_1.isClientManagerRunning()) {
							receivedMessage = (Message_Interface) clientManager_1.readMessage();
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
	 * Description: 				Verify that once the readMessage() function is called, a message from TCPserver is being read from an input object stream
	 * Internal variables TBV:		inputStream
	 * External variables TBV:		ServerMessage_SensorInfoQuerry, Message_Interface
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1));
		Thread.sleep(10);
		
		assertTrue(receivedMessage instanceof ServerMessage_SensorInfoQuerry);
	}
		
   /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that once the readMessage() function is called, it hangs in the getInputReaderStream().readObject() function until it receives a new message from TCPserver
	 * Internal variables TBV:		inputStream
	 * External variables TBV:		ServerMessage_SensorInfoQuerry, Message_Interface
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver
		testThread_readMessages.start();
		
		Message_Interface receivedMessage_old = null;
		Message_Interface receivedMessage_new = null;
			
		assertEquals(null, 							receivedMessage);
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1));
		Thread.sleep(10);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_old = receivedMessage;
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1));
		Thread.sleep(10);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_new = receivedMessage;
		
		assertNotEquals(receivedMessage_new, 		receivedMessage_old);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that SocketException is thrown if the client socket for an input object stream was closed while
	 								the readMessage() function was expecting for a new message from TCPserver",
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer()
	 * Exceptions thrown TBV:		SocketException
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@Test
	public void test_run_3() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		String expected_exception_name = "java.net.SocketException";
		
		// test thread that listens for exceptions caused on the client side
		testThread_exception = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					clientManager_1.readMessage();
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
		Thread.sleep(10);
		
		mockTCPclientTest.getClientSocket().close();
		Thread.sleep(10);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_4
	 * Description: 				Verify that ClassCastException is thrown if the message received by the readMessage() function is not a message of the Message_Interface type
	 * Internal variables TBV:		inputStream
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
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
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(10);
		
		String expected_exception_name = "java.lang.ClassCastException";
		
		// test thread that listens for exceptions caused on the client side
		testThread_exception = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					clientManager_1.readMessage();
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
		Thread.sleep(10);
		
		mockComputeEngine_Runnable.getOutputStream().writeObject(new String(""));
		Thread.sleep(10);
	}
	
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+ReadMessageTest.testID+" teardown section:");
	   
	   if(clientManager_1.isClientManagerRunning()) {
		   clientManager_1.setClientManagerRunning(false);
	   }
	   if(  (clientManager_1.getInputReaderStream() != null) && (ReadMessageTest.testID < 3) ) {
		   mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1));
		   clientManager_1.closeInStream();
	   }
	   else {
		   clientManager_1.closeInStream();
	   }
	   if (clientManager_1.getInputReaderStream() != null) {
		   clientManager_1.closeInStream();
	   }
	   if (clientManager_1.getOutputStream() != null){
		   clientManager_1.closeOutStream();
	   }
	   if(testThread_readMessages != null) {
		   if (testThread_readMessages.isAlive()) {
			   testThread_readMessages.interrupt();
		   }
	   }
	   if(testThread_exception != null) {
		   if (testThread_exception.isAlive()) {
			   testThread_exception.interrupt();
		   }
	   }
	   if (mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}
}
