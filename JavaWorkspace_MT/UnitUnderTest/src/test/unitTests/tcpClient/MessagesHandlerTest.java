package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
import messages.ClientMessage_ACK;
import messages.ClientMessage_SensorInfo;
import messages.Message_Interface;
import messages.ServerMessage_ACK;
import messages.ServerMessage_SensorInfoQuerry;
import sensor.SensorImpl;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;

public class MessagesHandlerTest {
	
	ClientManager clientManager_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    int sensor_ID_2 = 2;
    String serverHostName  = "localhost";
    Thread testThread_server = null;
    Thread testThread_client = null;
    Thread testThread_exception = null;
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
	
	String[] testPurpose = { "Verify that once the messagesHandler() function is called while the isClientManagerRunning flag is equal to true, the client manager class instance that runs the messagesHandler() function is able to read messages send from TCPserver",
							 "Verify that once the messagesHandler() function is called while the isClientManagerRunning flag is set to false, the client manager class instance that runs the messagesHandler() function is NOT able to read messages send from TCPserver",
							 "Verify that once the messagesHandler() function is called, it hangs in the readMessage() function until it gets a new message from TCPserver. \nIt is verified also that the state machine of messagesHandler() function is executed for all messages received from TCPserver, but every time the currently processing message is different from the previous",
							 "Verify that once the messagesHandler() function receives any message from TCPserver that has different sensor ID than the sensor ID that is written to the Client Manager class instance, \nthe state machine of messagesHandler() function sends ClientMessage_SensorInfo with the new sensor ID to confirm that this sensor ID was set intentionally. Verify also that the new sensor ID is also set for the sensor in Client_Sensors_LIST."};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		MessagesHandlerTest.testID += 1;
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
		
		// test thread that listens for messages on the server side
		testThread_server = new Thread(new Runnable() {
		//Runnable serverTask = new Runnable() {
		public void run() {
				try {
					while(clientManager_1.isClientManagerRunning()) {
						receivedMessage = (Message_Interface) (mockComputeEngine_Runnable.readMessage(mockComputeEngine_Runnable.getInputReaderStream()));
					}
				} catch (ClassNotFoundException e) {
					// To prove that exception's stack trace reported by JUnit caught ClassNotFoundException
					e.printStackTrace();
				} catch (IOException e) {
					// To prove that exception's stack trace reported by JUnit caught IOException
					e.printStackTrace();
				}
			}
		});
		
		// test thread that listens for messages on the client side and resends particular responses for the received messages
		testThread_client = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				clientManager_1.messagesHandler(clientManager_1.getOutputStream(), clientManager_1.getInputReaderStream());
			}
		});
		
		System.out.println("\t\tTest Run "+MessagesHandlerTest.testID+" Purpose:");
		System.out.println(testPurpose[(MessagesHandlerTest.testID-1)]);
		System.out.println("\t\tTest Run "+MessagesHandlerTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that once the messagesHandler() function is called while the isClientManagerRunning flag is equal to true,
	  								the client manager class instance that runs the messagesHandler() function is able to read messages send from TCPserver
	 * Internal variables TBV:		inputStream, outputStream, isClientManagerRunning
	 * External variables TBV:		ClientMessage_SensorInfo, ServerMessage_ACK, ServerMessage_SensorInfoQuerry
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
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
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertTrue(receivedMessage instanceof ClientMessage_SensorInfo);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
	}

   /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that once the messagesHandler() function is called while the isClientManagerRunning flag is set to false,
	  								the client manager class instance that runs the messagesHandler() function is NOT able to read messages send from TCPserver
	 * Internal variables TBV:		inputStream, outputStream, isClientManagerRunning
	 * External variables TBV:		ClientMessage_ACK, ClientMessage_SensorInfo, ServerMessage_ACK, ServerMessage_SensorInfoQuerry
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
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
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertTrue(receivedMessage instanceof ClientMessage_ACK);
		
		assertFalse(clientManager_1.isClientManagerRunning());
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertFalse(receivedMessage instanceof ClientMessage_SensorInfo);
	}
	
   /***********************************************************************************************************
	 * Test Name: 					test_run_3
	 * Description: 				Verify that once the messagesHandler() function is called, it hangs in the readMessage() function until it gets a new message from TCPserver.
	 								It is verified also that the state machine of messagesHandler() function is executed for all messages received from TCPserver,
	 								but every time the currently processing message is different from the previous
	 * Internal variables TBV:		inputStream, outputStream
	 * External variables TBV:		ServerMessage_SensorInfoQuerry
	 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
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
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		Message_Interface receivedMessage_old = null;
		Message_Interface receivedMessage_new = null;
			
		assertEquals(null, 							receivedMessage);
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_old = receivedMessage;
		
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_1), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		assertNotEquals(null, 						receivedMessage);
		receivedMessage_new = receivedMessage;
		
		assertNotEquals(receivedMessage_new, 		receivedMessage_old);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
	}
	
	   /***********************************************************************************************************
		 * Test Name: 					test_run_4
		 * Description: 				Verify that once the messagesHandler() function receives any message from TCPserver that has different sensor ID than the sensor ID
		 								that is written to the Client Manager class instance, the state machine of messagesHandler() function sends ClientMessage_SensorInfo
		 								with the new sensor ID to confirm that this sensor ID was set intentionally. Verify also that the new sensor ID is also set for the sensor in Client_Sensors_LIST.
		 * Internal variables TBV:		inputStream, outputStream
		 * External variables TBV:		ServerMessage_SensorInfoQuerry, ClientMessage_SensorInfo, ServerMessage_ACK, SensorImpl.sensorID, Client_Sensors_LIST
		 * Mocked objects:				TCPclient, TCPserver, ComputeEngine_Runnable, Socket
		 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.readMessage(), ComputeEngine_Runnable.sendMessage()
	     * Exceptions thrown: 			IOException, InterruptedException
		 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_4() throws IOException, InterruptedException {
		
		// bind server socket and start TCPserver
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		when(mockTCPclientTest.getClientSocket()).thenReturn(TCPclientSocket);	
		
		clientManager_1 = clientManager_1.initClientManager(mockTCPclientTest.getClientSocket(), sensor_ID_1);
		Thread.sleep(20);
		
		// start test Thread on the server side that is responsible for listening messages sent by TCPclient
		testThread_server.start();
		Thread.sleep(20);
		
		// start test Thread on the client side that is responsible for listening messages sent by TCPserver and re-sending particular responses that are verified in the consecutive test runs
		// method under test messagesHandler() is called in this thread
		testThread_client.start();
		Thread.sleep(20);
		
		SensorImpl temp_sens_old = mockTCPclientTest.searchInClientSensorList(sensor_ID_1);
	
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_SensorInfoQuerry(sensor_ID_2), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
		
		SensorImpl temp_sens_new = mockTCPclientTest.searchInClientSensorList(sensor_ID_2);
		
		assertTrue(receivedMessage instanceof ClientMessage_SensorInfo);
		assertEquals(sensor_ID_2, 			receivedMessage.getSensorID());
		assertEquals(temp_sens_old, 		temp_sens_new);		
		
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_2, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		Thread.sleep(20);
	}
	
	@SuppressWarnings("static-access")
	@After
	public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+MessagesHandlerTest.testID+" teardown section:");
	   
	   if (clientManager_1.getInputReaderStream() != null) {
		   clientManager_1.closeInStream();
	   }
	   if (clientManager_1.getOutputStream() != null){
		   clientManager_1.closeOutStream();
	   }
	   if(testThread_server != null) {
		   if (testThread_server.isAlive()) {
			   testThread_server.interrupt();
		   }
	   }
	   if(testThread_client != null) {
		   if (testThread_client.isAlive()) {
			   testThread_client.interrupt();
		   }
	   }
	   if (mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   for (int index = 0; index < mockTCPclientTest.Client_Sensors_LIST.size(); index++) {
		   mockTCPclientTest.Client_Sensors_LIST.remove(index);
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}

}
