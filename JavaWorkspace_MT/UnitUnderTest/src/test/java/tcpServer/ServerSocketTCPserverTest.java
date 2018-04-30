package tcpServer;

import static org.junit.Assert.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServerSocketTCPserverTest {

		
	int port_1 = 9876;
	TCPserver tcpserver_1 = null;
	int port_2 = 9877;
	Thread testThread = null;
	ServerSocket serverSocket_1 = null;
	ServerSocket serverSocket_2 = null;
	
	String[] testPurpose = { 	"Verify that once the server is created, its socket is being bound to the server port",
								"Verify that once the server is created, its indicator for getReuseAddress is set to true",
								"Verify that if a server instance at some port was closed, in case the new server instance at this port is created, it will have different socket ID",
								"Verify that there is the SocketException returned if there was an attempt to close a socket that runs a thread currently blocked in serverSocket.accept()",
								"Verify that there is the SocketException returned if there was an attempt to bind a server socket to the same port even if the server socket has been closed",
								"Verify that there is the SocketException returned if there was an attempt to bind a server socket to different port",
								"Verify that there is the SocketException returned if there was an attempt to bind a server socket to a port, to which a different server socket has been bound"};

	static int testID = 1;
	
	public static void incrementTestID() {
		ServerSocketTCPserverTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		
		if(ServerSocketTCPserverTest.testID == 4) {
			testThread = new Thread();
		}
		if(ServerSocketTCPserverTest.testID > 4) {
			serverSocket_1 = new ServerSocket();
			serverSocket_2 = new ServerSocket();
		}
		
		System.out.println("\t\tTest Run "+ServerSocketTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(ServerSocketTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+ServerSocketTCPserverTest.testID+" Logic:");
	}
	
	@Test
	public void test_run_1() throws IOException {
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isBound());
	}
	
	@Test
	public void test_run_2() throws IOException {
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().getReuseAddress());
	}
	
	@Test
	public void test_run_3() throws IOException {
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());

		ServerSocket oldSocket = tcpserver_1.getServerSocket();
		
		tcpserver_1.closeServer(tcpserver_1, port_1);
		assertFalse(tcpserver_1.isServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isClosed());
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());
		
		ServerSocket newSocket = tcpserver_1.getServerSocket();

		assertNotEquals(oldSocket,	newSocket);

	}

	@Test()
	public void test_run_4() throws IOException, InterruptedException {
		
		tcpserver_1 = tcpserver_1.initServer(port_1);
		assertTrue(tcpserver_1.isServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
		        public void run() {
		        	try {
		        		tcpserver_1.getServerSocket().accept();
		        	} catch (SocketException Sockex) {
		        		testThread.interrupt();
		        		assertTrue(testThread.isInterrupted());

		        	} catch (IOException IOex) {
		        		assertTrue(false);
		        	}
		        }
		});
		testThread.start();
		
		tcpserver_1.closeServer(tcpserver_1, port_1);
					
	}
	
	@Test(expected = SocketException.class)
	public void test_run_5() throws IOException {
		
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    
	    assertTrue(serverSocket_1.isBound());
	    
	    serverSocket_1.close();
	    serverSocket_1.bind(new java.net.InetSocketAddress(port_1));

	}
	
	@Test(expected = SocketException.class)
	public void test_run_6() throws IOException {
		
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    
	    assertTrue(serverSocket_1.isBound());
	    
	    serverSocket_1.bind(new java.net.InetSocketAddress(port_2));

	}
	
	@Test(expected = SocketException.class)
	public void test_run_7() throws IOException {

	    serverSocket_1.bind(new java.net.InetSocketAddress(port_2));
	    
	    assertTrue(serverSocket_1.isBound());
	    
	    serverSocket_2.bind(new java.net.InetSocketAddress(port_2));

	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+ServerSocketTCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(tcpserver_1 != null) {
		   if(tcpserver_1.isServerRunning()){
			   tcpserver_1.closeServer(tcpserver_1, port_1);
		   }
		   
	   }
	   if(serverSocket_1 != null){
		   serverSocket_1.close();
	   }
	   if(serverSocket_2 != null){
		   serverSocket_2.close();
	   }
	   
	   incrementTestID();
    }
	
	
}
