package tcpClient;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import sensor.SensorImpl;

public class SearchInClientSensorListTest {
	
	TCPclient tcpclient_1 = null;
	SensorImpl sensor_1  = null;
	SensorImpl sensor_2  = null;
	int sensor_ID_1 = 1;
	int sensor_ID_2 = 2;
	
	String[] testPurpose = { "Verify that once the searchInClientSensorListTest() function is called with a sensor ID for the SensorImpl class instance that exists Client_Sensors_LIST, this SensorImpl instance is returned",
						     "Verify that once the searchInClientSensorListTest() function is called with a sensor ID for the SensorImpl class instance that does not exits Client_Sensors_LIST, \"null\" is returned"};
						
	static int testID = 1;
	
	public static void incrementTestID() {
		SearchInClientSensorListTest.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		tcpclient_1 = new TCPclient();

		System.out.println("\t\tTest Run "+SearchInClientSensorListTest.testID+" Purpose:");
		System.out.println(testPurpose[(SearchInClientSensorListTest.testID-1)]);
		System.out.println("\t\tTest Run "+SearchInClientSensorListTest.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the searchInClientSensorListTest() function is called with a sensor ID for the SensorImpl class instance that exists Client_Sensors_LIST,
	 							this SensorImpl instance is returned
	 * Internal variables TBV: 	Client_Sensors_LIST
	 * External variables TBV: 	SensorImpl
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() {

		 sensor_1 = new SensorImpl(sensor_ID_1);
		 
		 tcpclient_1.updateClientSensorList(sensor_1);
		 
		 assertEquals(sensor_1, 	tcpclient_1.searchInClientSensorList(sensor_ID_1));
		 
		 sensor_1.setSensorID(sensor_ID_2);
		 tcpclient_1.updateClientSensorList(sensor_1);
		 
		 assertEquals(sensor_1, 	tcpclient_1.searchInClientSensorList(sensor_ID_2));
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the searchInClientSensorListTest() function is called with a sensor ID for the SensorImpl class instance that does not exits Client_Sensors_LIST, 
	 							\"null\" is returned
	 * Internal variables TBV: 	Client_Sensors_LIST
	 * External variables TBV: 	SensorImpl
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {

		 sensor_1 = new SensorImpl(sensor_ID_1);
		  
		 assertEquals(null, 		tcpclient_1.searchInClientSensorList(sensor_ID_1));
		 
		 tcpclient_1.updateClientSensorList(sensor_1);
		 
		 assertEquals(sensor_1, 	tcpclient_1.searchInClientSensorList(sensor_ID_1));
		 
		 tcpclient_1.setSensor_ID(sensor_ID_2);
		 
		 assertEquals(null, 		tcpclient_1.searchInClientSensorList(sensor_ID_2));
	}

	@SuppressWarnings("static-access")
	@After
	public void teardown() throws IOException, InterruptedException{

		System.out.println("\t\tTest Run "+SearchInClientSensorListTest.testID+" teardown section:");
		
		if(sensor_1 != null) {
			tcpclient_1.Client_Sensors_LIST.remove(sensor_1);
		}
		if(sensor_2 != null) {
			tcpclient_1.Client_Sensors_LIST.remove(sensor_2);
		}

		// Time offset between consecutive test runs execution
		Thread.sleep(100);

		incrementTestID();
	}

}
