package tcpClient;

import static org.junit.Assert.*;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sensor.SensorImpl;
import watchdog.Local_1h_Watchdog;

public class UpdateClientSensorListTest {

	TCPclient tcpclient_1 = null;
	SensorImpl sensor_1  = null;
	SensorImpl sensor_2  = null;
	int sensor_ID_1 = 1;
	int sensor_ID_2 = 2;
	
	String[] testPurpose = { 	"Verify that once the updateClientSensorListTest() function is called for the SensorImpl class instance with a sensor ID that does not exist in Client_Sensors_LIST, this SensorImpl instance is added to Client_Sensors_LIST",
							    "Verify that once the updateClientSensorListTest() function is called for the SensorImpl class instance that already exists in Client_Sensors_LIST, this SensorImpl instance is updated and saved to Client_Sensors_LIST at the same index"};
							
	static int testID = 1;
	
	public static void incrementTestID() {
		UpdateClientSensorListTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpclient_1 = new TCPclient();

		System.out.println("\t\tTest Run "+UpdateClientSensorListTest.testID+" Purpose:");
		System.out.println(testPurpose[(UpdateClientSensorListTest.testID-1)]);
		System.out.println("\t\tTest Run "+UpdateClientSensorListTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the updateClientSensorListTest() function is called for the SensorImpl class instance with a sensor ID that do not exist in Client_Sensors_LIST,
	  							this SensorImpl instance is added to Client_Sensors_LIST
	 * Internal variables TBV: 	Client_Sensors_LIST
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() {

		 sensor_1 = new SensorImpl(sensor_ID_1);
		 
		 tcpclient_1.updateClientSensorList(sensor_1);
		 
		 int expected_list_size_1 = 1;
		 
		 assertEquals(sensor_1, 				tcpclient_1.searchInClientSensorList(sensor_ID_1));
		 assertEquals(expected_list_size_1, 	tcpclient_1.Client_Sensors_LIST.size());
		 
		 sensor_2 = new SensorImpl(sensor_ID_2);
		 
		 tcpclient_1.updateClientSensorList(sensor_2);
		 
		 int expected_list_size_2 = 2;
		 
		 assertEquals(sensor_1, 				tcpclient_1.searchInClientSensorList(sensor_ID_1));
		 assertEquals(sensor_2, 				tcpclient_1.searchInClientSensorList(sensor_ID_2));
		 assertEquals(expected_list_size_2, 	tcpclient_1.Client_Sensors_LIST.size());
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the updateClientSensorListTest() function is called for the SensorImpl class instance that already exists in Client_Sensors_LIST,
	  							this SensorImpl instance is updated and saved to Client_Sensors_LIST at the same index
	 * Internal variables TBV: 	Client_Sensors_LIST
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {

		 sensor_1 = new SensorImpl(sensor_ID_1);
		 
		 tcpclient_1.updateClientSensorList(sensor_1);
		 
		 int expected_list_size_1 = 1;
		 
		 assertEquals(sensor_1, 				tcpclient_1.searchInClientSensorList(sensor_ID_1));
		 assertEquals(expected_list_size_1, 	tcpclient_1.Client_Sensors_LIST.size());
		 
		 sensor_1.setSensorID(sensor_ID_2);
		 
		 tcpclient_1.updateClientSensorList(sensor_1);
		 
		 assertEquals(sensor_1, 				tcpclient_1.searchInClientSensorList(sensor_ID_2));
		 assertEquals(expected_list_size_1, 	tcpclient_1.Client_Sensors_LIST.size());
	}

	@SuppressWarnings("static-access")
	@After
	public void teardown() throws IOException, InterruptedException{

		System.out.println("\t\tTest Run "+UpdateClientSensorListTest.testID+" teardown section:");
		
		if(sensor_1 != null) {
			tcpclient_1.Client_Sensors_LIST.remove(sensor_1);
		}
		if(sensor_2 != null) {
			tcpclient_1.Client_Sensors_LIST.remove(sensor_2);
		}
	   if(Local_1h_Watchdog.getInstance() != null) {
		   Local_1h_Watchdog.getInstance().setM_instance(null);
	   }

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
	}
	
}
