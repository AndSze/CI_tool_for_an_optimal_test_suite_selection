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
	
	String[] testPurpose = { "Verify that once the TCPserver class instance with a server socket ID is closed, the server socket ID for new TCPserver class instance is different than the previous server socket ID",
							 "Verify that in case of an attempt to close a server socket that runs a thread currently blocked in serverSocket.accept(), the Socket Exception is returned",
							 "Verify that in case of an attempt to bind a server socket to the same port even if the server socket has been closed, the Socket Exception is returned",
							 "Verify that in case of an attempt to bind a server socket that has been already bound to any different port, the Socket Exception is returned",
							 "Verify that in case of an attempt to bind a server socket to the port, to which a different server socket has been bound, the Socket Exception is returned"};

	static int testID = 1;
	
	public static void incrementTestID() {
		ServerSocketTCPserverTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		
		if(ServerSocketTCPserverTest.testID == 2) {
			testThread = new Thread();
		}
		if(ServerSocketTCPserverTest.testID > 2) {
			serverSocket_1 = new ServerSocket();
			serverSocket_2 = new ServerSocket();
		}
		
		System.out.println("\t\tTest Run "+ServerSocketTCPserverTest.testID+" Purpose:");
		System.out.println(testPurpose[(ServerSocketTCPserverTest.testID-1)]);
		System.out.println("\t\tTest Run "+ServerSocketTCPserverTest.testID+" Logic:");
	}

    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the TCPserver class instance with a server socket ID is closed, 
	  							the server socket ID for new TCPserver class instance is different than the previous server socket ID
	 * Internal variables TBV: 	serverSocket
     * Exceptions thrown:		IOException 
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() throws IOException {
		
		tcpserver_1 = new TCPserver(port_1);
		assertTrue(tcpserver_1.get_ServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());

		ServerSocket oldSocket = tcpserver_1.getServerSocket();
		
		tcpserver_1.closeServer(tcpserver_1, port_1);
		assertFalse(tcpserver_1.get_ServerRunning());
		assertTrue(tcpserver_1.getServerSocket().isClosed());
		
		tcpserver_1 = new TCPserver(port_1);
		assertTrue(tcpserver_1.get_ServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());
		
		ServerSocket newSocket = tcpserver_1.getServerSocket();

		assertNotEquals(oldSocket,	newSocket);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that in case of an attempt to close a server socket that runs a thread currently blocked in serverSocket.accept(), the Socket Exception is returned
	 * Internal variables TBV: 	serverSocket
	 * Exceptions thrown TBV:	SocketException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test()
	public void test_run_2() throws IOException {
		
		tcpserver_1 = new TCPserver(port_1);
		assertTrue(tcpserver_1.get_ServerRunning());
		assertFalse(tcpserver_1.getServerSocket().isClosed());
		
		String expected_exception_name = "java.net.SocketException";
		
		testThread = new Thread(new Runnable() {
			//Runnable serverTask = new Runnable() {
		        public void run() {
		        	try {
		        		tcpserver_1.getServerSocket().accept();
		        	} catch (SocketException e) {
		        		testThread.interrupt();
		        		// To prove that exception's stack trace reported by JUnit caught SocketException
		        		assertTrue(testThread.isInterrupted());
		        		assertEquals(expected_exception_name,		e.getClass().getName());
		        	} catch (IOException e) {
		        		// To prove that exception's stack trace reported by JUnit did not catch IOException
		        		assertTrue(false);
		        	}
		        }
		});
		testThread.start();
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that in case of an attempt to bind a server socket to the same port even if the server socket has been closed, the Socket Exception is returned
	 * Internal variables TBV: 	serverSocket
	 * Exceptions thrown TBV:	SocketException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_3() throws IOException {
		
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    
	    assertTrue(serverSocket_1.isBound());
	    
	    serverSocket_1.close();
	    serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    
		// To prove that exception's stack trace reported by JUnit caught SocketException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_4
	 * Description: 			Verify that in case of an attempt to bind a server socket that has been already bound to any different port, the Socket Exception is returned
	 * Internal variables TBV: 	serverSocket
	 * Exceptions thrown TBV:	SocketException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_4() throws IOException {
		
		serverSocket_1.bind(new java.net.InetSocketAddress(port_1));
	    
	    assertTrue(serverSocket_1.isBound());
	    
	    serverSocket_1.bind(new java.net.InetSocketAddress(port_2));
	    
		// To prove that exception's stack trace reported by JUnit caught SocketException
		assertTrue(false);
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_5
	 * Description: 			Verify that in case of an attempt to bind a server socket to the port, to which a different server socket has been bound, the Socket Exception is returned
	 * Internal variables TBV: 	serverSocket
	 * Exceptions thrown TBV:	SocketException
	 ***********************************************************************************************************/
	@Test(expected = SocketException.class)
	public void test_run_5() throws IOException {

	    serverSocket_1.bind(new java.net.InetSocketAddress(port_2));
	    
	    assertTrue(serverSocket_1.isBound());
	    
	    serverSocket_2.bind(new java.net.InetSocketAddress(port_2));

		// To prove that exception's stack trace reported by JUnit caught SocketException
		assertTrue(false);
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	  
	   System.out.println("\t\tTest Run "+ServerSocketTCPserverTest.testID+" teardown section:");
	   	   
	   // Time offset between consecutive test runs execution
	   Thread.sleep(1000);
	   
	   if(tcpserver_1 != null) {
		if(TCPserver.get_ServerRunning()){
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
