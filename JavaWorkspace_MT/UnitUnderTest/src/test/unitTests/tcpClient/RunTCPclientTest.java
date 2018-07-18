package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import messages.ClientMessage_BootUp;
import messages.Message_Interface;
import messages.ServerMessage_ACK;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;

public class RunTCPclientTest {

	TCPclient tcpclient_1 = null;
    int port_1 = 9876;
    int sensor_ID_1 = 1;
    String serverHostName  = "localhost";
    Thread testThread = null;
    
	// to mock TCPserver instances with ServerSocket mocks
	TCPserver mockTCPserverTest = null;
	ServerSocket tempServerSocket_1 = null;
	ServerSocket tempServerSocket_2 = null;
	
	// to mock Client Socket instance in the mockClientSocket = servSocket.accept() statement
	Socket mockClientSocket = null;
	
	// to mock client socket and client manager (for the purpose of avoiding configuration via calling the TCPclient class overloaded constructor)
	Socket TCPclientSocket = null;
	ClientManager mockClientManger = null;
	ObjectOutputStream tempOutputStream = null;
	ObjectInputStream tempInputStream = null;
	
	ObjectInputStream servInputStream = null;
	ObjectOutputStream servOutputStream = null;
	Message_Interface receivedMessage = null;
	
	// to mock server threads
	Thread mockServerThread = null;
	ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	
	String[] testPurpose = { "Verify that the run() function for the TCPclass instance handles its runnable functionality - it is verified by starting the TCPclient thread and proving that the run() function is executed",
							 "Verify that once the runTheClientTest() function is called, a dedicated thread that handles TCP connection is started for the TCPclient class instance",
							 "Verify that UnknownHostException is thrown once the runTheClientTest() function is called on a host name that is not allowed to set up a TCP connection",
							 "Verify that ConnectException is thrown once the runTheClientTest() function is called while attempting to connect a socket to a remote address and port. Typically, the connection was refused remotely",
							 "Verify that IOException is thrown once the runTheClientTest() function is called when the bind operation fails, or if the socket is already bound"};
	
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
		//mockClientManger = mock(ClientManager.class);
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
									//mockComputeEngine_Runnable = Mockito.spy(new ComputeEngine_Runnable(mockClientSocket, 1.0, false));
									//auxiliaryServerThreadExecutor.submit(mockComputeEngine_Runnable);
									//Message_Interface receivedMessage = (Message_Interface) (mockComputeEngine_Runnable.getInputReaderStream()).readObject();
									servOutputStream = new ObjectOutputStream(mockClientSocket.getOutputStream());
									servInputStream = new ObjectInputStream(mockClientSocket.getInputStream());
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
	 * Description: 				Verify that the run() function for the TCPclass instance handles its runnable functionality - it is verified by starting the TCPclient thread
	  								and proving that the run() function activates the Compute Engine Runnable interface on the TCPserver side
	 * Internal variables TBC:		clientThread						
	 * Called internal functions: 	clientThread.start()
	 * Mocked internal objects: 	clientManager, clientSocket
	 * Mocked external methods: 	TCPserver.startServer()
     * Exceptions thrown: 			IOException, InterruptedException
     * @throws ClassNotFoundException 
	 ***********************************************************************************************************/
	@SuppressWarnings("unused")
	@Test
	public void test_run_1() throws IOException, InterruptedException, ClassNotFoundException {
		
		mockTCPserverTest.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest.startServer(mockTCPserverTest.getServerSocket());
		mockServerThread.start();
		
		TCPclientSocket = new Socket(serverHostName, port_1);
		tcpclient_1.setClientSocket(TCPclientSocket);
		
		/*
		tempOutputStream = new ObjectOutputStream(TCPclientSocket.getOutputStream());
		tempInputStream = new ObjectInputStream(TCPclientSocket.getInputStream());
		when(mockClientManger.getInputReaderStream()).thenReturn(tempInputStream);
		when(mockClientManger.getOutputStream()).thenReturn(tempOutputStream);
		*/
		mockClientManger = new ClientManager();
		mockClientManger = mockClientManger.initClientManager(TCPclientSocket, sensor_ID_1);
		
		tcpclient_1.setClientManager(mockClientManger);
		Thread.sleep(100);
		
		//assertFalse(mockComputeEngine_Runnable.get_ComputeEngineRunning());
		//mockComputeEngine_Runnable.set_ComputeEngineRunning(false);
		
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
			public void run() {
				try {
					receivedMessage = (Message_Interface) (servInputStream).readObject();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		testThread.start();
		
		Thread TCPclient_thread = new Thread(tcpclient_1, "TCPclient Thread");
		TCPclient_thread.start();
		tcpclient_1.setClientThread(TCPclient_thread);
		Thread.sleep(100);

		Thread.sleep(1000);
		
		assertTrue(receivedMessage instanceof ClientMessage_BootUp);
		
		//assertTrue(mockComputeEngine_Runnable.get_ComputeEngineRunning());
		
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+RunTCPclientTest.testID+" teardown section:");
	   
	   if(mockTCPserverTest.getServerSocket().isBound()) {
		   mockTCPserverTest.getServerSocket().close();
	   }
	   if (tcpclient_1.getClientManager() != null) {
		   tcpclient_1.closeClientManager(tcpclient_1);
	   }
	   if (tcpclient_1.getClientSocket() != null) {
		   tcpclient_1.closeClient(tcpclient_1);
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }

}
