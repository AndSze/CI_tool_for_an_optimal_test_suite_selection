package deliverables;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
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
	
	@Test(expected=SocketException.class)
	public void Test_2(){
		System.out.println("This test verifies that the server can be created successfully at any port");
		//assertEquals("This test verifies that the server can be created successfully at any port", outContent.toString());
		
		UUT_TCPserver uut_TCPserver1 = new UUT_TCPserver(9876);
		
		UUT_TCPserver.runTheServer(uut_TCPserver1.getINSTANCE(), uut_TCPserver1.getPort());
		
		UUT_TCPserver uut_TCPserver2 = new UUT_TCPserver(9877);
		
		UUT_TCPserver.runTheServer(uut_TCPserver2.getINSTANCE(), uut_TCPserver1.getPort());
		
		assertNotEquals(uut_TCPserver1.getPort(),uut_TCPserver2.getPort());
		assertEquals(uut_TCPserver1.getINSTANCE(),uut_TCPserver2.getINSTANCE());
	}
	
   @After
    public void teardown(){
	   TCPserver.getInstance().closeServer(port);
    }

}




