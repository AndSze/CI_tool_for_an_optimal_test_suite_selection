package deliverables;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tcpServer.TCPserver;
import tcpServer.TCPserverIntegrationTest;
import tcpServer.TCPserverTest;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class ApplicationSanityCheckIT {

	UUT_TCPclient[] uut_tcp_clients;
	UUT_TCPserver uut_tcp_server_1 = null;
    int port = 9876;
    String serverHostName  = "localhost";
	
	int port_1 = 9876;
	
	String[] testPurpose = { "Verify that once the overloaded constructor of the TCPserver class is called, all files from previous communication via a TCP connection are deleted from Sensors_PATH",
							 "Verify that once the overloaded constructor of the TCPserver class is called, information about each sensor instance created in accordance with the TCPserver class attributes is saved in .sensor_info file with current timestamp and _sensorINITIALIZATION extension",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the .sensor_info files contain information about each sensor instance that is consisten with the TCPserver class attributes"};
	static int testID = 1;
	
	public static void incrementTestID() {
		ApplicationSanityCheckIT.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		uut_tcp_server_1 = new UUT_TCPserver(port);
		
		uut_tcp_clients = new UUT_TCPclient[TCPserver.get_1hWatchog_timestamp_table().get().length];
		for(int i = 0; i < uut_tcp_clients.length; i++) {
			System.out.println("\t\t Creating uut_tcp_clients: ["+(i+1)+"]");
			uut_tcp_clients[i] = new UUT_TCPclient(i+1, port, serverHostName);
		}
		
		System.out.println("\t\tTest Run "+ApplicationSanityCheckIT.testID+" Purpose:");
		System.out.println(testPurpose[(ApplicationSanityCheckIT.testID-1)]);
		System.out.println("\t\tTest Run "+ApplicationSanityCheckIT.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 * 							the TCPserver class instance is being updated with new server socket that is bound to the port and has ReuseAddress set to TRUE
	 * Requirements TBV:		XXX
     * @throws IOException 
     * @throws InterruptedException 
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() throws IOException, InterruptedException {
		
		uut_tcp_server_1.main(null);
		Thread.sleep(1000);
		
		for(int i = 0; i < uut_tcp_clients.length; i++) {
			System.out.println("\t\t Launching main method for uut_tcp_clients: ["+(uut_tcp_clients[i].getSensor_ID())+"]");
			System.out.println("\t\t uut_tcp_clients length: ["+uut_tcp_clients.length+"]");
			
			uut_tcp_clients[i].main(i+1, port, serverHostName);
			
			Thread.sleep(1000);
		}
		while( (Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 0) && (Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 0 ) ){
			Thread.sleep(100);
		}
	}

   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+ApplicationSanityCheckIT.testID+" teardown section:");
	   
	   /*
	   Thread.sleep(Global_1h_Watchdog.getInstance().getExpiration()*TCPserver.getMeasurements_limit());
	   
	   uut_tcp_server_1.closeTheServer();
	   for(int i = 0; i < uut_tcp_clients.length; i++) {
			uut_tcp_clients[i].closeClientManager(uut_tcp_clients[i].getINSTANCE());
			uut_tcp_clients[i].closeClient(uut_tcp_clients[i].getINSTANCE());
		}
	   */
	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
}
