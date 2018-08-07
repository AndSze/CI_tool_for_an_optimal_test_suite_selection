package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
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
import messages.ClientMessage_ACK;
import messages.ClientMessage_BootUp;
import messages.Message_Interface;
import messages.ServerMessage_ACK;
import sensor.SensorImpl;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Local_1h_Watchdog;

public class RunTCPclientTest {

	TCPclient tcpclient_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    String serverHostName  = "localhost";
    Thread testThread = null;
    
    // to make reading messages sent by Client Manger possible, the ClientManager class instance cannot be a mock (due to final methods that are not supported by Mockito)
    ClientManager tempClientManger = null;
    
    // placeholder for messages sent by the ClientManager class instance
    Message_Interface receivedMessage = null;
    
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket_1 = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock client socket and client manager (for the purpose of avoiding configuration via calling the TCPclient class overloaded constructor)
	Socket TCPclientSocket = null;

	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	
	String[] testPurpose = { "Verify that the run() function for the TCPclient class instance handles its runnable functionality - it is verified by proving that once the TCPclient thread is started, the run() function is executed and sends the ClientMessage_BootUp message",
							 "Verify that once the TCPclient thread is started, the run() function activates the messagesHandler() function for the ClientManager class instance - it is verified by proving that the expected ClientMessage_ACK message is received as a response for the ServerMessage_ACK message"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		RunTCPclientTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpclient_1 = new TCPclient();
		
		// mock Server Socket to enable the Client Socket to establish connection
		mockTCPserverTest = mock(TCPserver.class);
		mockClientSocket = mock(Socket.class);
		mockComputeEngine_Runnable = mock(ComputeEngine_Runnable.class);
		
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
		
		System.out.println("\t\tTest Run "+RunTCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(RunTCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+RunTCPclientTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_1
	 * Description: 				Verify that the run() function for the TCPclient class instance handles its runnable functionality - it is verified by proving that
	   								once the TCPclient thread is started, the run() function is executed and sends the ClientMessage_BootUp message
	 * Internal variables TBV:		clientThread
	 * Mocked objects:				TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		tcpclient_1.setClientSocket(TCPclientSocket);
		tcpclient_1.updateClientSensorList(new SensorImpl(sensor_ID_1));
		
		tempClientManger = new ClientManager();
		tempClientManger = tempClientManger.initClientManager(TCPclientSocket, sensor_ID_1);
		
		tcpclient_1.setClientManager(tempClientManger);
		Thread.sleep(20);
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					receivedMessage = (Message_Interface) (mockComputeEngine_Runnable.getInputReaderStream()).readObject();
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
		testThread.start();
		Thread.sleep(20);
		
		Thread TCPclient_thread = new Thread(tcpclient_1, "TCPclient Thread");
		TCPclient_thread.start();
		tcpclient_1.setClientThread(TCPclient_thread);
		Thread.sleep(50);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());

		assertTrue(receivedMessage instanceof ClientMessage_BootUp);
	}
	
    /***********************************************************************************************************
	 * Test Name: 					test_run_2
	 * Description: 				Verify that once the TCPclient thread is started, the run() function activates the messagesHandler() function for the ClientManager class instance - it is verified
	 * 								by proving that the expected ClientMessage_ACK message is received as a response for the ServerMessage_ACK message
	 * Internal variables TBV:		clientThread
	 * Mocked objects:				TCPserver, ComputeEngine_Runnable, Socket
	 * Mocks methods called:		TCPserver.startServer(), ComputeEngine_Runnable.sendMessage()
     * Exceptions thrown: 			IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() throws IOException, InterruptedException, ClassNotFoundException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		tcpclient_1.setClientSocket(TCPclientSocket);
		tcpclient_1.updateClientSensorList(new SensorImpl(sensor_ID_1));
		
		tempClientManger = new ClientManager();
		tempClientManger = tempClientManger.initClientManager(TCPclientSocket, sensor_ID_1);
		
		tcpclient_1.setClientManager(tempClientManger);
		Thread.sleep(20);
		
		Thread TCPclient_thread = new Thread(tcpclient_1, "TCPclient Thread");
		TCPclient_thread.start();
		tcpclient_1.setClientThread(TCPclient_thread);
		Thread.sleep(20);
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					while (tempClientManger.isClientManagerRunning()) {
						receivedMessage = (Message_Interface) (mockComputeEngine_Runnable.getInputReaderStream()).readObject();
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
		testThread.start();
		Thread.sleep(20);
		
		// send ServerMessage_ACK message with respective watchdog values to close TCP connection - it is required to close ClientManager with no ConnectException thrown
		mockComputeEngine_Runnable.sendMessage(new ServerMessage_ACK(sensor_ID_1, mockComputeEngine_Runnable.getLocal_1h_watchdog() ,mockComputeEngine_Runnable.getLocal_24h_watchdog()), mockComputeEngine_Runnable.getOutputStream());
		
		Thread.sleep(20);
		assertTrue(receivedMessage instanceof ClientMessage_ACK);
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+RunTCPclientTest.testID+" teardown section:");
	   
	   if (tcpclient_1.getClientManager() != null) {
		   tcpclient_1.closeClientManager(tcpclient_1);
	   }
	   if (tcpclient_1.getClientSocket() != null) {
		   tcpclient_1.closeClient(tcpclient_1);
	   }
	   if (testThread.isAlive()) {
		   testThread.interrupt();
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
