package tcpServer;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpClient.ClientManager;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;;

public class StartServerTCPserverTest {
	
		
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	TCPserver tcpserver_2 = null;
	ServerSocket serverSocket_1 = null;
	ServerSocket serverSocket_2 = null;
	Socket clientSocket = null;
	ClientManager mockClinetManagerTest = null;
	final String serverHostName = "localhost";
	
	String[] testPurpose = { 	"Verify that once the start server function is called, there is a new server thread created",
								"Verify that if there are multiple server instances created at different ports, each is processed in a different thread",
								"Verify that once the server thread is started it has the RUNNABLE thread state. Verify also that once the server socket is closed, the server thread state changes to the TERMINATED thread state",
								"Verify that there is no Thread started for processing the client messages unless there has been any output stream for the client socket created",
								"Verify that once an output stream for the client socket is createad, all messages sent via this output stream are being handled in a separated thred",
								"Verify that there is a possibility to create up to 8 threads to process client messages"};
	static int testID = 1;
	
	public static void incrementTestID() {
		StartServerTCPserverTest.testID += 1;
	}	
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		serverSocket_1 = new ServerSocket();
		
		if(StartServerTCPserverTest.testID == 2) {
		tcpserver_2 = new TCPserver();
		serverSocket_2 = new ServerSocket();
		}
		if(StartServerTCPserverTest.testID > 4) {
			mockClinetManagerTest = mock(ClientManager.class);
		}
		System.out.println("\t\tTest Run "+StartServerTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(StartServerTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+StartServerTCPserverTest.testID+" Logic:");
	}
	
	@Test
	public void test_run_1() throws IOException {
		
		serverSocket_1.setReuseAddress(true);
	    serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    tcpserver_1.ServerRunning(true);

		assertEquals(null, 		tcpserver_1.getServerThread());
		
		tcpserver_1.startServer(serverSocket_1);
		assertNotEquals(null, 	tcpserver_1.getServerThread());
		
	}
	
	@Test
	public void test_run_2() throws IOException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));    
	    tcpserver_1.ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);
		Thread testThread1 = tcpserver_1.getServerThread();
		serverSocket_2.bind(new java.net.InetSocketAddress(port_2));
	    tcpserver_2.ServerRunning(true);
		tcpserver_2.startServer(serverSocket_2);
		Thread testThread2 = tcpserver_2.getServerThread();

		assertNotEquals(testThread2, 	testThread1);
	}
	
	@Test
	public void test_run_3() throws IOException, InterruptedException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));    
	    tcpserver_1.ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);
		
		assertEquals(Thread.State.RUNNABLE,		tcpserver_1.getServerThread().getState());
		
		serverSocket_1.close();
		Thread.sleep(100);
		
		assertEquals(Thread.State.TERMINATED,	tcpserver_1.getServerThread().getState());
	}
	
	@Test
	public void test_run_4() throws IOException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		tcpserver_1.ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);
		clientSocket = new Socket(serverHostName, port_1);
		
		assertEquals(0,	tcpserver_1.getThreadPoolExecutor().getActiveCount());

	}
	
	@Test
	public void test_run_5() throws IOException, InterruptedException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		tcpserver_1.ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);			
		clientSocket = new Socket(serverHostName, port_1);
	    try {
	    	
	    	// there is no need to do the below object initialization in its class constructor !!!
	        //when(mockClinetManagerTest.initClientManager(clientSocket)).thenReturn(new ClientManager ());
	        //when(mockClinetManagerTest.getInputReaderStream()).thenReturn(new InputStreamReader(clientSocket.getInputStream()));
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new PrintStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		mockClinetManagerTest.getOutputStream().print("message 1 to be sent"); 
		Thread.sleep(100);
		
		assertEquals(1,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
		
		clientSocket = new Socket(serverHostName, port_1);
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new PrintStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		mockClinetManagerTest.getOutputStream().print("message 2 to be sent"); 
		Thread.sleep(100);
		
		assertEquals(2,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
	}
	
	@Test
	public void test_run_6() throws IOException, InterruptedException {
		
		serverSocket_1.setReuseAddress(true);
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
		tcpserver_1.ServerRunning(true);
		tcpserver_1.startServer(serverSocket_1);			
		clientSocket = new Socket(serverHostName, port_1);
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new PrintStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		mockClinetManagerTest.getOutputStream().print("message 1 to be sent"); 
		Thread.sleep(100);
		
		assertEquals(1,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
		
		for (int i = 2; i < 9 ; i++) {
			clientSocket = new Socket(serverHostName, port_1);
		    try {
		        when(mockClinetManagerTest.getOutputStream()).thenReturn(new PrintStream(clientSocket.getOutputStream()));
		    } catch (IOException e) {
		        fail(e.getMessage());
		    }
			mockClinetManagerTest.getOutputStream().print("message "+i+" to be sent");
			Thread.sleep(100);
		}
		assertEquals(8,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
		
		clientSocket = new Socket(serverHostName, port_1);
	    try {
	        when(mockClinetManagerTest.getOutputStream()).thenReturn(new PrintStream(clientSocket.getOutputStream()));
	    } catch (IOException e) {
	        fail(e.getMessage());
	    }
		mockClinetManagerTest.getOutputStream().print("message 9 to be sent");
		Thread.sleep(100);
		
		assertEquals(8,	tcpserver_1.getThreadPoolExecutor().getActiveCount());
	}

	@After
	public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+StartServerTCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(serverSocket_1 != null){
		   serverSocket_1.close();
	   }
	   if(serverSocket_2 != null){
		   serverSocket_2.close();
	   }
	   
	   incrementTestID();
	}


}
