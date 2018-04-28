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
	TCPserver tcpserver_2 = null;
	Thread testThread = null;

	
	String[] testPurpose = { 	"Verify that once the server is created, its socket is being bound to the server port",
								"Verify that once the server is created, its indicator for getReuseAddress is set to true",
								"Verify that if a server instance at some port was closed, in case the new server instance at this port is created, it will have different socket ID",
								"Verify that there is the SocketException returned if there was an attempt to close a socket that runs a thread currently blocked in serverSocket.accept()"};
	static int testID = 1;
	
	public static void incrementTestID() {
		ServerSocketTCPserverTest.testID += 1;
	}
	
	
	@Before
	public void before() throws IOException {
		tcpserver_1 = new TCPserver();
		tcpserver_2 = new TCPserver();
		testThread = new Thread();

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

		assertNotEquals(oldSocket,newSocket);

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
	   if(tcpserver_2 != null){
		   if(tcpserver_2.isServerRunning()){
			   tcpserver_2.closeServer(tcpserver_2, port_2);
		   
		   }
	   }
	   
	   incrementTestID();
    }
	
	
}
