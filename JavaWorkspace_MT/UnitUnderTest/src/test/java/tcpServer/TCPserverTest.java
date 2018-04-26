package tcpServer;

import static org.junit.Assert.*;
import org.junit.Test;
import deliverables.UUT_TCPserver;

import java.io.IOException;
import java.net.BindException;

public class TCPserverTest {

	@Test(expected = BindException.class)
	public void test() throws IOException {
		TCPserver tcpserver1 = new TCPserver();
		tcpserver1.initServer(9876);
		TCPserver tcpserver2 = new TCPserver();
		tcpserver2.initServer(9876);
		
		int a = 0;
	}
}
