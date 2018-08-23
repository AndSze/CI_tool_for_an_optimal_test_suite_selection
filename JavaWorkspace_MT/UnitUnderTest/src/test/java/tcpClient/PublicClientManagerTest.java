package tcpClient;

import static org.junit.Assert.*;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import watchdog.Local_1h_Watchdog;


public class PublicClientManagerTest {

	ClientManager clientManager_1 = null;
	
	String[] testPurpose = { "Verify that once the default constructor of the ClientManager class is being called, an instance of the ClientManager class is created with its attributes set to default values"};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		PublicClientManagerTest.testID += 1;
	}

	@Before
	public void before() throws IOException {
		
		System.out.println("\t\tTest Run "+PublicClientManagerTest.testID+" Purpose:");
		System.out.println(testPurpose[(PublicClientManagerTest.testID-1)]);
		System.out.println("\t\tTest Run "+PublicClientManagerTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the default constructor of the ClientManager class is being called, an instance of the ClientManager class is created with its attributes set to default values
	 * Internal variables TBV: 	outputStream, inputStream, isClientManagerRunning, sensor_ID
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() {
		
		clientManager_1 = new ClientManager();
		
		int default_sensor_ID = 0;
		
		assertEquals(null,						clientManager_1.getInputReaderStream());	
		assertEquals(null,						clientManager_1.getOutputStream());			
		assertEquals(default_sensor_ID,			clientManager_1.sensor_ID);	
		assertFalse(clientManager_1.isClientManagerRunning());

	}
	
	
   @After
   public void teardown() throws IOException, InterruptedException{
	   
	   	System.out.println("\t\tTest Run "+PublicClientManagerTest.testID+" teardown section:");
   
	   if(Local_1h_Watchdog.getInstance() != null) {
		   Local_1h_Watchdog.getInstance().setM_instance(null);
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
   }

}
