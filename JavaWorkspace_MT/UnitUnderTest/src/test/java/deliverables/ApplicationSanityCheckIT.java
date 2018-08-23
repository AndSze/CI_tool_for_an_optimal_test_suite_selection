package deliverables;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tcpServer.TCPserver;
import tcpServer.TCPserver_Teardown;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class ApplicationSanityCheckIT {

	UUT_TCPclient[] uut_tcp_clients;
	UUT_TCPserver uut_tcp_server_1 = null;
    int port = 8765;
    int number_of_sensors = 8;
    double watchdog_scale_factor = 0.002;
    int measurements_limit = 24;
    String serverHostName  = "localhost";
    
    // in seconds
    double _1h_watchdog_kick_time_intervals = 3600 * watchdog_scale_factor;
    double _24h_watchdog_kick_time_intervals = 3600 * watchdog_scale_factor * measurements_limit;
    double _1h_time_counter = 0.0;
    double _24h_time_counter = 0.0;
    
    int seconds_to_milliseconds_conv_factor = 1000;
    double milliseconds_to_seconds_conv_factor = 0.001;
	
	String[] testPurpose = { "Verify that once the application is stared it is endlessly sending messages via TCP connection between TCPserver and multiple TCPclients  in parallel threads. \nVerification is done for application outputs that are serialized files saved after each TCP connection cycle on PC disc."};
	
	static int testID = 1;
	
	public static void incrementTestID() {
		ApplicationSanityCheckIT.testID += 1;
	}
	
	@Before
	public void before() throws IOException {
		
		uut_tcp_server_1 = new UUT_TCPserver(port, number_of_sensors, measurements_limit, watchdog_scale_factor);
		
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
	 * Description: 			Verify that once the application is stared it is endlessly sending messages via TCP connection between TCPserver and multiple TCPclients  in parallel threads. 
	 							Verification is done for application outputs that are serialized files saved after each TCP connection cycle on PC disc
	 * Exceptions thrown:		IOException, InterruptedException
	 ***********************************************************************************************************/
	@SuppressWarnings("static-access")
	@Test
	public void test_run_1() throws IOException, InterruptedException {

		uut_tcp_server_1.main(port, number_of_sensors, measurements_limit, watchdog_scale_factor);
		Thread.sleep(1000);

		for(int i = 0; i < uut_tcp_clients.length; i++) {
			System.out.println("\t\t Launching main method for uut_tcp_clients: ["+(uut_tcp_clients[i].getSensor_ID())+"]");
			System.out.println("\t\t uut_tcp_clients length: ["+uut_tcp_clients.length+"]");

			uut_tcp_clients[i].main(i+1, port, serverHostName);

			Thread.sleep(100);
		}
		
	    // define threshold for test execution pass
	    int successful_TCPconenctions_threshold = 3;
	    int successful_TCPconnections_number = 0;
	    boolean initial_check = true;
	    
		// execute the test until watchodgs have expired or any of comparison statement has failed 
		// otherwise, if the test reaches 3 successful whole TCP connection cycles ( whole cycle means that _1h_Watchdog is kicked measurement_limit times and _24h_Watchdog is kicked once)
		while(true){
			
			int cut_seconds = 3;
			int cut_minutes = 6;
			double cut_minutes_threshold = 3.0;
			int number_of_files = 0;
			
			if ((Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 0) && (Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 0 )) {
				
				double startTime = System.currentTimeMillis();

				// test for sensor initialization logic
				if(_1h_time_counter == 0.0 && initial_check) {

					String temp_timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
					String temp_timestamp_sec = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
					String temp_timestamp_no_mins = new SimpleDateFormat("yyyy-MM-dd_HH").format(Calendar.getInstance().getTime());

					// in case files are serialized just after a new minute has stared - ignore check of minutes in files' timestamps due to possibility of mismatch between test and serialization timestamps 
					boolean ignore_minutes = false;				
					if( Integer.parseInt(temp_timestamp_sec.substring(temp_timestamp_sec.length() - 2)) < cut_minutes_threshold) {
						System.out.println("Integer.parseInt(temp_timestamp_sec: " + (temp_timestamp_sec.substring(temp_timestamp_sec.length() - 2)) );
						ignore_minutes = true;
					}

					String expected_file_name = null;
					String expected_extension_name = null;
					// in alphabetical order
					String expected_extension_name_1 = "_sensorINITIALIZATION.sensor_info";
					String expected_extension_name_2 = "_gotoMAINTENANCEafterINITIALIZATION.sensor_info";
					String expected_extension_name_3 = "_gotoOPERATIONALafterCONFIGURATION.sensor_info";
					String expected_extension_name_4 = "_stayinOPERATIONAL.sensor_info";

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
						String[] entries_2 = file_path.list();
						for (int j = 0; j< entries_2.length; j++) {
							// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
							new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
							files_to_be_verified_path = new java.io.File(new_path);

							int loop_counter = 1;
							for (File file :  files_to_be_verified_path.listFiles()) {
								System.out.println("[ApplicationSanityCheckIT sensor: " +(i+1)+ "] Sensor Info check file.getName(): \t" + file.getName());
								if(loop_counter == 1) {
									expected_extension_name = expected_extension_name_1;
								}
								else if(loop_counter == 2){
									expected_extension_name = expected_extension_name_2;
								}
								else if(loop_counter == 3){
									expected_extension_name = expected_extension_name_3;
								}
								else if(loop_counter == 4){
									expected_extension_name = expected_extension_name_4;
								}

								if(ignore_minutes){
									expected_file_name = entries_1[i] + "_" + temp_timestamp_no_mins;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name.length() + cut_minutes)));
								}
								else {
									expected_file_name = entries_1[i] + "_" + temp_timestamp;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name.length() + cut_seconds)));
								}

								loop_counter += 1;
								// verification of the file name is divided to two parts due to the fact that the file name contains timestamp with seconds
								// snippet of the file name with seconds is not verified
								assertEquals(expected_extension_name, 	file.getName().substring(file.getName().length() - expected_extension_name.length()));
							}
						}
					}

					initial_check = false;
				}		
				
				// test for measurement data logic (executed every time _1h_Watchdog is kicked)
				if(_1h_time_counter >= _1h_watchdog_kick_time_intervals) {

					String temp_timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
					String temp_timestamp_sec = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
					String temp_timestamp_no_mins = new SimpleDateFormat("yyyy-MM-dd_HH").format(Calendar.getInstance().getTime());

					// in case files are serialized just after a new minute has stared - ignore check of minutes in files' timestamps due to possibility of mismatch between test and serialization timestamps 
					boolean ignore_minutes = false;
					if( Integer.parseInt(temp_timestamp_sec.substring(temp_timestamp_sec.length() - 2)) < cut_minutes_threshold) {
						System.out.println("Integer.parseInt(temp_timestamp_sec: " + (temp_timestamp_sec.substring(temp_timestamp_sec.length() - 2)) );
						ignore_minutes = true;
					}

					String expected_file_name = null;
					String expected_extension_name_sensorInfo = "_stayinOPERATIONAL.sensor_info";
					String expected_extension_name_measurementData = ".measurement_data";

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
						String[] entries_2 = file_path.list();
						for (int j = 0; j< entries_2.length; j++) {
							if(entries_2[j].equals("sensor_Infos")) {
								// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
								new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
								files_to_be_verified_path = new java.io.File(new_path);

								// check only the newest file
								number_of_files = files_to_be_verified_path.listFiles().length;
								File file = files_to_be_verified_path.listFiles()[number_of_files -1];

								System.out.println("[ApplicationSanityCheckIT sensor: " +(i+1)+ "] Sensor Info check file.getName(): \t\t" + file.getName());

								if(ignore_minutes){
									expected_file_name = entries_1[i] + "_" + temp_timestamp_no_mins;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_sensorInfo.length() + cut_minutes)));
								}
								else {
									expected_file_name = entries_1[i] + "_" + temp_timestamp;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_sensorInfo.length() + cut_seconds)));
								}

								// verification of the file name is divided to two parts due to the fact that the file name contains timestamp with seconds
								// snippet of the file name with seconds is not verified
								assertEquals(expected_extension_name_sensorInfo, 	file.getName().substring(file.getName().length() - expected_extension_name_sensorInfo.length()));
							}
							else if(entries_2[j].equals("measurement_Datas")){
								// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
								new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
								files_to_be_verified_path = new java.io.File(new_path);

								// check only the newest file
								number_of_files = files_to_be_verified_path.listFiles().length;
								File file = files_to_be_verified_path.listFiles()[number_of_files -1];

								System.out.println("[ApplicationSanityCheckIT sensor: " +(i+1)+ "] Measurement Data check file.getName(): \t" + file.getName());

								if(ignore_minutes){
									expected_file_name = "measurement" + "_" + temp_timestamp_no_mins;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_measurementData.length() + cut_minutes)));
								}
								else {
									expected_file_name = "measurement" + "_" + temp_timestamp;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_measurementData.length() + cut_seconds)));
								}

								// verification of the file name is divided to two parts due to the fact that the file name contains timestamp with seconds
								// snippet of the file name with seconds is not verified
								assertEquals(expected_extension_name_measurementData, 	file.getName().substring(file.getName().length() - expected_extension_name_measurementData.length()));
							
							}
						}	
					}

					_1h_time_counter = 0.0;
				}
				
				// test for measurement history logic (executed every time _24h_Watchdog is kicked)
				if(_24h_time_counter >= _24h_watchdog_kick_time_intervals) {

					String temp_timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(Calendar.getInstance().getTime());
					String temp_timestamp_sec = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
					String temp_timestamp_no_mins = new SimpleDateFormat("yyyy-MM-dd_HH").format(Calendar.getInstance().getTime());

					// in case files are serialized just after a new minute has stared - ignore check of minutes in files' timestamps due to possibility of mismatch between test and serialization timestamps 
					boolean ignore_minutes = false;
					if( Integer.parseInt(temp_timestamp_sec.substring(temp_timestamp_sec.length() - 1)) < cut_minutes_threshold) {
						System.out.println("Integer.parseInt(temp_timestamp_sec: " + Integer.parseInt(temp_timestamp_sec.substring(temp_timestamp_sec.length() - 1)) );
						ignore_minutes = true;
					}

					String expected_file_name = null;
					String expected_extension_name_sensorInfo = "_gotoOPERATIONALafterRESET.sensor_info";
					String expected_extension_name_measurementHistory = ".measurement_history";

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
						String[] entries_2 = file_path.list();
						for (int j = 0; j< entries_2.length; j++) {
							if(entries_2[j].equals("sensor_Infos")) {
								// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
								new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
								files_to_be_verified_path = new java.io.File(new_path);

								
								// check only the newest file
								number_of_files = files_to_be_verified_path.listFiles().length;
								File file = files_to_be_verified_path.listFiles()[number_of_files - 1];

								System.out.println("[ApplicationSanityCheckIT sensor: " +(i+1)+ "] Sensor Info check file.getName(): \t\t" + file.getName());

								if(ignore_minutes){
									expected_file_name = entries_1[i] + "_" + temp_timestamp_no_mins;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_sensorInfo.length() + cut_minutes)));
								}
								else {
									expected_file_name = entries_1[i] + "_" + temp_timestamp;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_sensorInfo.length() + cut_seconds)));
								}

								// verification of the file name is divided to two parts due to the fact that the file name contains timestamp with seconds
								// snippet of the file name with seconds is not verified
								assertEquals(expected_extension_name_sensorInfo, 	file.getName().substring(file.getName().length() - expected_extension_name_sensorInfo.length()));
							}
							else if(entries_2[j].equals("measurement_Datas")) {
								// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
								new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
								files_to_be_verified_path = new java.io.File(new_path);

								// check only the newest file
								number_of_files = files_to_be_verified_path.listFiles().length;
								int expected_number_of_files = 0;

								assertEquals(expected_number_of_files, 	number_of_files);
							}						
							else if(entries_2[j].equals("measurement_Histories")) {
								// new_path is a string that represents a file name in a folder in the Sensors_PATH directory
								new_path = TCPserver.getSensorsPath()  + "\\" + entries_1[i] + "\\" + entries_2[j];
								files_to_be_verified_path = new java.io.File(new_path);

								// check only the newest file
								number_of_files = files_to_be_verified_path.listFiles().length;
								File file = files_to_be_verified_path.listFiles()[number_of_files -1];

								System.out.println("[ApplicationSanityCheckIT sensor: " +(i+1)+ "] Measurement History check file.getName(): \t" + file.getName());

								if(ignore_minutes){
									expected_file_name = "measurements" + "_" + temp_timestamp_no_mins;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_measurementHistory.length() + cut_minutes)));
								}
								else {
									expected_file_name = "measurements" + "_" + temp_timestamp;
									assertEquals(expected_file_name, 	file.getName().substring(0, file.getName().length() - (expected_extension_name_measurementHistory.length() + cut_seconds)));
								}
								// verification of the file name is divided to two parts due to the fact that the file name contains timestamp with seconds
								// snippet of the file name with seconds is not verified
								assertEquals(expected_extension_name_measurementHistory, 	file.getName().substring(file.getName().length() - expected_extension_name_measurementHistory.length()));
							}
						}	
					}
					successful_TCPconnections_number += 1;
					_24h_time_counter = 0.0;
					_1h_time_counter = 0.0;
				}

				Thread.sleep(100);

				double stopTime = System.currentTimeMillis();
				double elapsedTime = stopTime - startTime;
				_1h_time_counter += elapsedTime * milliseconds_to_seconds_conv_factor;
				_24h_time_counter  += elapsedTime * milliseconds_to_seconds_conv_factor;

			}
			else {
				// force the test to fail if any of watchdog has expired and stop execution
				System.out.println("[ApplicationSanityCheckIT] failed due to Watchdog expiration");
				assertTrue(false);
				break;
			}
			if(successful_TCPconnections_number == successful_TCPconenctions_threshold) {
				// stop successful test execution since 3 whole TCP connection cycles have been launched without errors
				System.out.println("[ApplicationSanityCheckIT] passed since number of TCPconnections reached the threshold");
				break;
			}
		}
	}

   @After
    public void teardown() throws IOException, InterruptedException{
	   
	   System.out.println("\t\tTest Run "+ApplicationSanityCheckIT.testID+" teardown section:");
	   
	   // run the reinitalize_to_default() function that sets all attributes of a static class TCPserver to default
	   TCPserver_Teardown tcp_server_teardown = new TCPserver_Teardown();
	   tcp_server_teardown.reinitalize_to_default(TCPserver.getInstance(port, number_of_sensors, measurements_limit, watchdog_scale_factor));

	   // Time offset between consecutive test runs execution
	   Thread.sleep(100);
	   
	   System.out.println("");
	   incrementTestID();
    }
}
