package deliverables;

import static org.junit.Assert.assertEquals;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import deliverables.UUT_TCPserver;
import sensor.SensorImpl;
import tcpServer.TCPserver;
import tcpServer.TCPserverTest;
import tcpServer.TCPserver_Teardown;

public class TCPserverIT {
	
	UUT_TCPserver uut_tcp_server_1 = null;
    int port = 9876;
    int number_of_sensors = 8;
    double watchdog_scale_factor = 0.001;
    int measurements_limit = 24;
	
	String[] testPurpose = { "Verify that once the overloaded constructor of the TCPserver class is called, all files from previous communication via a TCP connection are deleted from Sensors_PATH",
							 "Verify that once the overloaded constructor of the TCPserver class is called, information about each sensor instance created in accordance with the TCPserver class attributes is saved in .sensor_info file with current timestamp and _sensorINITIALIZATION extension",
							 "Verify that once the overloaded constructor of the TCPserver class is called, the .sensor_info files contain information about each sensor instance that is consisten with the TCPserver class attributes"};
	static int testID = 1;
	
	public static void incrementTestID() {
		TCPserverIT.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		uut_tcp_server_1 = new UUT_TCPserver(port, number_of_sensors, measurements_limit, watchdog_scale_factor);
		
		System.out.println("\t\tTest Run "+TCPserverIT.testID+" Purpose:");
		System.out.println(testPurpose[(TCPserverIT.testID-1)]);
		System.out.println("\t\tTest Run "+TCPserverIT.testID+" Logic:");
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_1
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, 
	 * 							the TCPserver class instance is being updated with new server socket that is bound to the port and has ReuseAddress set to TRUE
	 * Requirements TBV:		XXX
	 ***********************************************************************************************************/
	@Test
	public void test_run_1() {
		
		String new_path = null;
		File file_path = null;
		File files_to_be_deleted_path = null;
		file_path = new java.io.File(TCPserver.getSensorsPath());
		
		// entries_1 is an array of strings that consists of the files names in the Sensors_PATH directory
		String[]entries_1 = file_path.list();
		for (int i = 0; i< entries_1.length; i++) {
			// new_path is a string that represents a folder name in the Sensors_PATH directory
			new_path = TCPserver.getSensorsPath() + "\\" + entries_1[i];
			file_path = new java.io.File(new_path);
			// entries_2 is an array of strings that consists of the files names in each folder in the Sensors_PATH directory
			String[]entries_2 = file_path.list();
			for (int j = 0; j< entries_2.length; j++) {
				// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
				files_to_be_deleted_path = new java.io.File(new_path);
				// the sensor_Infos files are created once the overloaded constructor of the TCPserver class is called, 
				// hence there will be new sensor_Infos files created after deleting all files from previous TCP communication session, 
				// hence if a folder name equals "sensor_Infos", the assertEquals condition is not executed for this for loop iteration
				if(entries_2[j].equals("sensor_Infos")) continue;
				// if an array returned by listFiles() is empty, an evidence that all files have been deleted is given
				assertEquals(0, files_to_be_deleted_path.listFiles());
			}
		}
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_2
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, information about each sensor instance created
	 							in accordance with the TCPserver class attributes is saved in .sensor_info file with current timestamp and _sensorINITIALIZATION extension
	 * Requirements TBV:		XXX
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_2() {
		
		String temp_timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
		String expected_file_name = null;
		String expected_extension_name = "_sensorINITIALIZATION.sensor_info";
		
		String new_path = null;
		File file_path = null;
		File files_to_be_verified_path = null;
		file_path = new java.io.File(TCPserver.getSensorsPath());
		
		// entries_1 is an array of strings that consists of the files names in the Sensors_PATH directory
		String[]entries_1 = file_path.list();
		for (int i = 0; i< entries_1.length; i++) {
			// new_path is a string that represents a folder name in the Sensors_PATH directory
			new_path = TCPserver.getSensorsPath() + "\\" + entries_1[i];
			file_path = new java.io.File(new_path);
			// entries_2 is an array of strings that consists of the files names in each folder in the Sensors_PATH directory
			String[]entries_2 = file_path.list();
			for (int j = 0; j< entries_2.length; j++) {
				// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
				new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
				files_to_be_verified_path = new java.io.File(new_path);
				expected_file_name = entries_1[i] + "_" + temp_timestamp + expected_extension_name;
				for (File file :  files_to_be_verified_path.listFiles()) {
					System.out.println("file.getName(): \t" + file.getName());
					assertEquals(expected_file_name, 	file.getName());// .substring(0, file.getName().length() - 24));
					// verification of the file name is divided to two parts due to the fact that the file name contains timestamp with seconds
					// snippet of the file name with seconds is not verified
					assertEquals(expected_extension_name, 	file.getName().substring(file.getName().length() - 33));
				}
			}
		}
	}
	
    /***********************************************************************************************************
	 * Test Name: 				test_run_3
	 * Description: 			Verify that once the overloaded constructor of the TCPserver class is called, information about each sensor instance created
	 							in accordance with the TCPserver class attributes is saved in .sensor_info file with current timestamp
	 * Requirements TBV:		XXX
	 * Exceptions thrown:		ClassNotFoundException 
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_3() throws ClassNotFoundException {
		
		SensorImpl temp_sensor = null;
		
		String new_path = null;
		File file_path = null;
		File files_to_be_verified_path = null;
		file_path = new java.io.File(TCPserver.getSensorsPath());
		
		// entries_1 is an array of strings that consists of the files names in the Sensors_PATH directory
		String[]entries_1 = file_path.list();
		for (int i = 0; i< entries_1.length; i++) {
			// new_path is a string that represents a folder name in the Sensors_PATH directory
			new_path = TCPserver.getSensorsPath() + "\\" + entries_1[i];
			file_path = new java.io.File(new_path);
			// entries_2 is an array of strings that consists of the files names in each folder in the Sensors_PATH directory
			String[]entries_2 = file_path.list();
			for (int j = 0; j< entries_2.length; j++) {
				// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
				new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
				files_to_be_verified_path = new java.io.File(new_path);
				for (String fileNames : files_to_be_verified_path.list()) {
					fileNames = "C:\\Projects\\Test_Integration_Jenkins\\CI_tool\\master\\JavaWorkspace_MT\\UnitUnderTest\\" + new_path  + "\\" +  fileNames;
					System.out.println(fileNames);
					temp_sensor = (SensorImpl) TCPserver.getProcessing_engine().deserialize(fileNames, SensorImpl.class);
					
					assertEquals(i+1,																temp_sensor.getSensorID());
					assertEquals(new Point2D.Float(TCPserver.getSensor_coordinates_array()[i][0], 	TCPserver.getSensor_coordinates_array()[i][1]),	
																									temp_sensor.getCoordinates());
					assertEquals(TCPserver.getMeasurements_limit(),									temp_sensor.getSensor_m_history_array_size());
					assertEquals(TCPserver.getWatchdogs_scale_factor(),								temp_sensor.getLocal_watchdog_scale_factor(), 0.001);
					assertEquals(TCPserver.getSoftwareImageID(),									temp_sensor.getSoftwareImageID());
				}
			}
		}
	}
	
   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+TCPserverIT.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(TCPserver.getInstance(port, number_of_sensors, measurements_limit, watchdog_scale_factor));

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
}
