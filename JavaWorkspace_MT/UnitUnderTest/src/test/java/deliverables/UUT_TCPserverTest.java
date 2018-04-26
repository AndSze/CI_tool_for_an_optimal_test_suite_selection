package deliverables;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import tcpServer.TCPserver;


public class UUT_TCPserverTest {
	
	private int port = 9876;
	//private UUT_TCPserver uut_TCPserver1= null;
	//private UUT_TCPserver uut_TCPserver2= null;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

	
	/*
	 * 1) Verify that the server can be created successfully at any port
	 * 2) Verify that the Bind Exception is returned if there was an attempt to create the server at the same port twice
	 * 3) Verify that multiple servers can be created at different ports
	 */

	@Before
	public void before() {
		
	}
	
	
	@Test
	public void Test_1() throws IOException{
		System.out.println("This test verifies that the server can be created successfully at any port");
		//assertEquals("This test verifies that the server can be created successfully at any port", outContent.toString());
		
		UUT_TCPserver uut_TCPserver1 = new UUT_TCPserver(port);
		
		UUT_TCPserver.runTheServer(uut_TCPserver1.getINSTANCE(), uut_TCPserver1.getPort());
		
		//UUT_TCPserver uut_TCPserver2 = new UUT_TCPserver(9877);
		
		//UUT_TCPserver.runTheServer(uut_TCPserver2.getINSTANCE(), uut_TCPserver1.getPort());
		
		assertEquals(uut_TCPserver1.getPort(),port);
		assertNotEquals(uut_TCPserver1.getINSTANCE(),null);
		
		uut_TCPserver1.getINSTANCE().closeServer(port);
		
		int a = 0;
	}
	
	/*@Test()
	public void Test_2() throws IOException{
		System.out.println("This test verifies that the server can be created successfully at any port");
		//assertEquals("This test verifies that the server can be created successfully at any port", outContent.toString());
		
		UUT_TCPserver uut_TCPserver1 = new UUT_TCPserver(port+1);
		
		UUT_TCPserver.runTheServer(uut_TCPserver1.getINSTANCE(), uut_TCPserver1.getPort());
		
		//UUT_TCPserver uut_TCPserver2 = new UUT_TCPserver(9877);
		
		//UUT_TCPserver.runTheServer(uut_TCPserver2.getINSTANCE(), uut_TCPserver1.getPort());
		
		assertEquals(uut_TCPserver1.getPort(),port+1);
		assertNotEquals(uut_TCPserver1.getINSTANCE(),null);
	}
	*/
	
   @After
    public void teardown() throws IOException{
	   //TCPserver.getInstance().closeServer(port);
    }

}




