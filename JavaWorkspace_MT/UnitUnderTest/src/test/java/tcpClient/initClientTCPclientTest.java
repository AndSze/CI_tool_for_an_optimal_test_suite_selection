package tcpClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
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

import messages.ServerMessage_ACK;
import tcpServer.ComputeEngine_Runnable;
import tcpServer.TCPserver;
import watchdog._1h_Watchdog;

public class initClientTCPclientTest {

	int port_1 = 9876;
	int sensor_ID_1 = 1;
	TCPclient tcpclient_1 = null;
	int port_2 = 9877;
	int sensor_ID_2 = 2;
	TCPclient tcpclient_2 = null;
	int port_3 = 9877;
	TCPclient tcpclient_3 = null;
	String serverHostName = "localhost";
	TCPserver mockTCPserverTest_1 = null;
	ObjectOutputStream tempServer_OutputStream_1 = null;
	//_1h_Watchdog mock_Watchdog = null;
	ServerSocket tempServerSocket_1 = null;
	TCPserver mockTCPserverTest_2 = null;
	ServerSocket tempServerSocket_2 = null;
	Socket clientSocket = null;
	TCPclient mockTCPclientTest = null;
	//ObjectOutputStream tempObjectOutputStream;
	//ComputeEngine_Runnable mockComputeEngine_Runnable = null;
	
	ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ThreadPoolExecutor auxiliaryServerThreadExecutor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
	private Thread auxiliaryServerThread = null;

	
	String[] testPurpose = { 	"Verify that the Connect Exception is returned if there was an attempt to create the client without a server instance initialized previously",
								"Verify that once the server is initialized at any registered port, the client can be created successfully at this port",
								"Verify that once the server is initialized at any registered port, multiple client instances can be created successfully at the same port",
								"Verify that once multiple server instances are initialized at different at any registered port ports, multiple client instances can be created successfully at any port with a server instance initialized",
								"Verify that the Connect Exception caused by connection timeout is returned if there was an attempt to create the client at the IP address of the host, for which the connection cannot be established",
								"Verify that the Connect Exception is returned if there was an attempt to create the client at an invalid address on local machine, or port that is not valid on remote machine",
								//"Verify that the Unknkown Host Exception is returned if there was an attempt to create the client at the IP address of the host that can not be determined"
								};
								
	static int testID = 1;
	
	public static void incrementTestID() {
		initClientTCPclientTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpclient_1 = new TCPclient();

		if(initClientTCPclientTest.testID > 1) {
			// mock Server Socket to enable the Client Socket to establish connection
			mockTCPserverTest_1 = mock(TCPserver.class);
			//mockTCPclientTest = mock(TCPclient.class);
			//mockComputeEngine_Runnable = mock(ComputeEngine_Runnable.class);
			
			tempServerSocket_1 = new ServerSocket();
			when(mockTCPserverTest_1.getServerSocket()).thenReturn(tempServerSocket_1);
			
			
			//when(mockComputeEngine_Runnable.getOutputStream()).thenReturn((ObjectOutputStream) tempServerSocket_1.accept().getOutputStream());
			//when(mockTCPclientTest.getClientSocket()).thenReturn(tempServerSocket_1.accept());
		
			
			auxiliaryServerThread = new Thread(new Runnable() {
				//Runnable serverTask = new Runnable() {
			        public void run() {
			        	try {	
							Socket clientSocket = mockTCPserverTest_1.getServerSocket().accept();
							tempServer_OutputStream_1 = new ObjectOutputStream(clientSocket.getOutputStream());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							assertTrue(false);
						}
			            
			        }
			});
			
			/* OK, what you've got is a kind of "remote deadlock". 
			 * The ObjectInputStream on the client is waiting for the object stream from the server before proceeding, but the server isn't going to send that, 
			 * because its ObjectInputStream is waiting for the header from the client before proceeding... 
			 */
			
			//mockComputeEngine_Runnable_1 = mock(ComputeEngine_Runnable.class);
			//mock_Watchdog = mock(_1h_Watchdog.class);
			//tempServer_OutputStream_1 = new ObjectOutputStream(tcpclient_1.getClientSocket().getOutputStream());
			//when(mock_Watchdog.getTimeLeftBeforeExpiration()).thenReturn((double) 3600).thenReturn((double)3600*24);
		}
		
		if (initClientTCPclientTest.testID == 3 || initClientTCPclientTest.testID == 4)
		{
			tcpclient_2 = new TCPclient();
			tcpclient_3 = new TCPclient();
		}
		if (initClientTCPclientTest.testID == 4)
		{
			mockTCPserverTest_2 = mock(TCPserver.class);
			tempServerSocket_2 = new ServerSocket();
			when(mockTCPserverTest_2.getServerSocket()).thenReturn(tempServerSocket_2);
		}
		if (initClientTCPclientTest.testID == 5)
		{
			serverHostName = "1.1.1.1";
		}
		if (initClientTCPclientTest.testID == 6)
		{
			port_1 = 0;
		}
		if (initClientTCPclientTest.testID == 7)
		{
			serverHostName = "";
		}
		
		System.out.println("\t\tTest Run "+initClientTCPclientTest.testID+" Purpose:");
		System.out.println(testPurpose[(initClientTCPclientTest.testID-1)]);
		System.out.println("\t\tTest Run "+initClientTCPclientTest.testID+" Logic:");
	}
	
	@Test(expected = ConnectException.class)
	public void test_run_1() throws IOException {
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
	@Test
	public void test_run_2() throws IOException, InterruptedException {
		
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		mockTCPserverTest_1.ServerRunning(true);
		mockTCPserverTest_1.startServer(mockTCPserverTest_1.getServerSocket());
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		assertTrue(tcpclient_1.isClientRunning());
		

		//tempServer_OutputStream_1.writeObject(new ServerMessage_ACK(sensor_ID_1,mock_Watchdog.getTimeLeftBeforeExpiration(), mock_Watchdog.getTimeLeftBeforeExpiration()));
		
	}
	
	@Test
	public void test_run_3() throws IOException {
		
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		mockTCPserverTest_1.ServerRunning(true);
		mockTCPserverTest_1.startServer(mockTCPserverTest_1.getServerSocket());
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_1, serverHostName, port_1);
			
		tcpclient_3 = tcpclient_3.initClient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_2.isClientRunning());
		assertTrue(tcpclient_3.isClientRunning());
		
	}
	
	@Test
	public void test_run_4() throws IOException {
	
		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		mockTCPserverTest_2.getServerSocket().bind(new java.net.InetSocketAddress(port_2));
		
		
		mockTCPserverTest_1.ServerRunning(true);
		mockTCPserverTest_2.ServerRunning(true);
		mockTCPserverTest_1.startServer(mockTCPserverTest_1.getServerSocket());
		mockTCPserverTest_2.startServer(mockTCPserverTest_2.getServerSocket());
		 
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_1, serverHostName, port_1);
		tcpclient_3 = tcpclient_3.initClient(sensor_ID_1, serverHostName, port_1);
		
		assertTrue(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_2.isClientRunning());
		assertTrue(tcpclient_3.isClientRunning());
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_2, serverHostName, port_2);
		tcpclient_2 = tcpclient_2.initClient(sensor_ID_2, serverHostName, port_2);
		tcpclient_3 = tcpclient_3.initClient(sensor_ID_2, serverHostName, port_2);
		
		assertTrue(tcpclient_1.isClientRunning());
		assertTrue(tcpclient_2.isClientRunning());
		assertTrue(tcpclient_3.isClientRunning());
	}
	
	@Test(expected = ConnectException.class)
	public void test_run_5() throws IOException {

		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
	@Test(expected = ConnectException.class)
	public void test_run_6() throws IOException {

		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		
		tcpclient_1 = tcpclient_1.initClient(sensor_ID_1, serverHostName, port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the ConnectException
		assertTrue(false);
	}
	
	/*
	@Test(expected = UnknownHostException.class)
	public void test_run_6() throws IOException {

		mockTCPserverTest_1.getServerSocket().bind(new java.net.InetSocketAddress(port_1));
		 
		tcpclient_1 = tcpclient_1.initClient((new InetSocketAddress("google.com", 80)), port_1);
		
		// To prove that exception's stack trace reported by JUnit caught the UnknownHostException
		assertTrue(false);
	}*/


   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+initClientTCPclientTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(tcpclient_1 != null) {
		   if(tcpclient_1.isClientRunning()){
			   tcpclient_1.closeClient(tcpclient_1, port_1);
		   }
	   }
	   if(tcpclient_2 != null){
		   if(tcpclient_2.isClientRunning()){
			   tcpclient_2.closeClient(tcpclient_2, port_2);
		   
		   }
	   }
	   if(tcpclient_3 != null){
		   if(tcpclient_3.isClientRunning()){
			   tcpclient_3.closeClient(tcpclient_2, port_2);
		   
		   }
	   }
	   if(mockTCPserverTest_1 != null){
		   if(!mockTCPserverTest_1.getServerSocket().isClosed()) {
			   mockTCPserverTest_1.getServerSocket().close();
		   }
	   }
	   if(mockTCPserverTest_2 != null){
		   if(!mockTCPserverTest_2.getServerSocket().isClosed()) {
			   mockTCPserverTest_2.getServerSocket().close();
		   }
	   }
	   if(mockTCPserverTest_1 != null) {
		   if (mockTCPserverTest_1.getServerThread().isAlive()) {
			   mockTCPserverTest_1.closeServer(mockTCPserverTest_1, port_1);
		   }
	   }
	   if(mockTCPserverTest_2 != null) {
		   if (mockTCPserverTest_2.getServerThread().isAlive()) {
			   mockTCPserverTest_2.closeServer(mockTCPserverTest_2, port_1);
		   }
	   }
	   
	   incrementTestID();
    }
	
}


