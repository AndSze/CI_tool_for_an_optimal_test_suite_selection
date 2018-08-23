package tcpServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import messages.ClientMessage_ACK;
import messages.ClientMessage_BootUp;
import messages.ClientMessage_MeasurementData;
import messages.ClientMessage_MeasurementHistory;
import messages.ClientMessage_SensorInfo;
import messages.Message_Interface;
import messages.SensorState;
import messages.ServerMessage_ACK;
import messages.ServerMessage_Request_MeasurementData;
import messages.ServerMessage_Request_MeasurementHistory;
import messages.ServerMessage_SensorInfoQuerry;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.MeasurementData;
import sensor.SensorImpl;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;
import tcpServer.Watchdog_Thresholds;
import tcpServer.Delays;

public class ComputeEngine_Runnable extends TCPserver implements Runnable {
	
    /***********************************************************************************************************
	 * ComputeEngine_Runnable - Class Attributes
	 ***********************************************************************************************************/
    private ObjectOutputStream outputStream = null;
    private ObjectInputStream inputStream = null;
    private SensorImpl sensor = null;
	private boolean isComputeEngine_Runnable_running = false;
    private int delay = 0;
    private double local_1h_watchdog = 0;
    private double local_24h_watchdog = 0;
    // seconds_to_miliseconds_conversion_factor is introduced, because watchdogs are in [s] units, whereas delays are given in [ms]
    private final double seconds_to_miliseconds_conversion_factor = 1000;
    
	/***********************************************************************************************************
   	 * Auxiliary piece of code
   	 * Specific Variable Names: 	1) double local_watchdog_scale_factor
	 								2) int[] delays_array
	 								3) final int delays_array_size
	 								4) double[] watchdog_thresholds_array
	 								5) final int watchdog_thresholds_array_size
	 * Description: 				Interfaces for the testing purposes - to parameterize times dependencies
	 ***********************************************************************************************************/
    protected double local_watchdog_scale_factor = 1.0;
    protected int[] delays_array;
    protected final int delays_array_size = 4;
    protected double[] watchdog_thresholds_array;
    protected final int watchdog_thresholds_array_size = 4;
   
    /***********************************************************************************************************
	 * Method Name: 				public ComputeEngine_Runnable()
	 * Description: 				ComputeEngine_Runnable class default constructor
	 * Affected internal variables: outputStream, inputStream, isComputeEngine_Runnable_running, local_1h_watchdog, local_24h_watchdog, local_watchdog_scale_factor, delays_array, watchdog_thresholds_array
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	public ComputeEngine_Runnable(Socket clientSocket, double global_watchdog_scale_factor, boolean isComputeEngine_Runnable_running) throws IOException  {
		super();
		// create object output/input streams
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        
        // local watchdog factor has to be set prior to setting local watchdogs since those values need to be scaled with this factor
        setLocal_watchdog_scale_factor(global_watchdog_scale_factor);
        
        // set ComputeEngine_Runnable_running to indicate if TCP connection has to be launched
        setComputeEngine_Runnable_running(isComputeEngine_Runnable_running);
        
        // set delays in miliseconds that are determined based on watchdog_scale_factor
        setDelays_array(set_delays_array(global_watchdog_scale_factor, delays_array_size));
        
        // set watchdogs thresholds in secongs that are determined based on watchdog_scale_factor
        setWatchdog_thresholds_array(set_watchdog_thresholds_array(global_watchdog_scale_factor, watchdog_thresholds_array_size));
        
        // set 1h local watchdog to 1h global watchdog time left before expiration
        if( Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() < (get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, getWatchdog_thresholds_array())) && 
            Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > (get_watchdog_threshold(Watchdog_Thresholds.HIGH, getWatchdog_thresholds_array())) ) {
        	Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, getWatchdog_thresholds_array()));
        	setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        }
        else {
        	setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        }
        
    	// To be Deleted
    	int temp = 0;

        
        // set 24h local watchdog to 24h global watchdog time left before expiration
        setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        
        System.out.println("[Compute engine Runnable] Multithreaded Server Service has been started");
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private void sendMessage()
	 * Description: 				writes object that has to inherit from Message_Interface class to object output stream
	 * Affected internal variables: outputStream
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	public void sendMessage(Message_Interface message, ObjectOutputStream out_stream) throws IOException {
		
    	// To be Deleted
    	int temp_1 = 0;
    	
		if (out_stream != null) {
			// sends message from the server via its output stream to the client input stream
			out_stream.writeObject(message); 
		}
		else {
			throw new IllegalArgumentException();
		}   
	}
	
	/***********************************************************************************************************
	 * Method Name: 				public Message_Interface readMessage()
	 * Description: 				reads an object that has to inherit from Message_Interface class from object input stream
	 * Affected internal variables: inputStream
	 * Returned value:				Message_Interface
	 * Exceptions thrown: 			IOException, ClassNotFoundException
	 ***********************************************************************************************************/
	public Message_Interface readMessage(ObjectInputStream in_stream) throws IOException, ClassNotFoundException {
		
		Message_Interface receivedMessage = null;
		
		if (in_stream != null) {
			
			// reads message sent to the server input stream from the client output stream
			receivedMessage = (Message_Interface) in_stream.readObject();
		}
		else {
			throw new IllegalArgumentException();
		}   
		
		return receivedMessage;
	}
	
	/***********************************************************************************************************
	 * Method Name: 				public void run()
	 * Description: 				runnable state machine for massages sent to sensors based on received messages via TCP connection and watchdogs time left to expiration
	 * Affected internal variables: inputStream, outputStream, delay, local_1h_watchdog, local_24h_watchdog
	 * Affected external variables: SensorImpl, TCPserver._1hWatchog_timestamp_table, TCPserver._24hWatchog_timestamp_table, TCPserver.processing_engine, TCPserver.Server_Sensors_LIST,
	 								TCPserver.MeasurementHistory_LIST, TCPserver.MeasurementData_LIST
	 * Local variables:				close_ComputeEngine_Runnable, request_measurement_history, request_measurement_data, receivedMessage
	 * Called internal functions: 	setLocal_1h_watchdog(), setLocal_24h_watchdog, setDelay(), sendMessage(), closeOutStream(), closeInStream(), processingDelay()
	 * Called external functions: 	ServerMessage_SensorInfoQuerry(), ServerMessage_SensorInfoUpdate(), ServerMessage_ACK(), ServerMessage_Request_MeasurementData(), 
	 								ServerMessage_Request_MeasurementHistory(), ComputeEngine_Processing.updateServerSensorList(), ComputeEngine_Processing.saveSensorInfo(),
	 								ComputeEngine_Processing.saveMeasurementDataInfo(), ComputeEngine_Processing.saveMeasurementHistoryInfo(), SensorImpl.resetSensor(),
	 								SensorImpl.addMeasurement(), TCPserver.set_24hWatchog_Allfalse
	 ***********************************************************************************************************/
    public void run() {
    	
    	// flag that indicates that close_ComputeEngine_Runnable is going to be closed
    	boolean close_ComputeEngine_Runnable = false;
    	// flag that indicates that serverMessage_Request_MeasurementHistory is going to be send
    	boolean request_measurement_history = false;
    	// flag that indicates that serverMessage_Request_MeasurementData is going to be send
    	boolean request_measurement_data = false;
        // generic Message_Interface objects that will be casted to a particular message type once it is received
    	Message_Interface receivedMessage = null;
    	
    	try {
    		while(true) {
    			
    			setDelay(get_delays(Delays.LOW, getDelays_array()));
    			if(isComputeEngine_Runnable_running()) {
    				
					if( (receivedMessage = readMessage(getInputReaderStream())) != null) {
		    			
	    				sensor = getProcessing_engine().searchInServerSensorList(receivedMessage.getSensorID());
						
		    			if (receivedMessage instanceof ClientMessage_BootUp && sensor != null) {
		    				
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_BootUp from sensor: " + sensor.getSensorID() + " has been received.");
		    				sendMessage(new ServerMessage_SensorInfoQuerry(receivedMessage.getSensorID()), getOutputStream());
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_BootUp with ServerMessage_SensorInfoQuerry.");
		    			}
		    			else if (receivedMessage instanceof ClientMessage_ACK && sensor != null) {
		    				
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_ACK from sensor: " + sensor.getSensorID() + " has been received.");
		    				if (getLocal_1h_watchdog() < (get_watchdog_threshold(Watchdog_Thresholds.HIGH, getWatchdog_thresholds_array())) ) {
		    					
		    					// adjust Local_1h_watchdogs based on its time left to expiration when ClientMessage_ACK has been received
			    				setLocal_1h_watchdog(_1h_Watchdog_close_to_expire(getLocal_1h_watchdog(), getLocal_watchdog_scale_factor(), sensor.getNumberOfMeasurements()));
		    					
			    				setDelay(get_delays(Delays.LOW, getDelays_array()));
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog global: \t" +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog global: \t" +  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog with delay for ServerMessage_Request_MeasurementData (Local_1h_watchdog): \t" + getLocal_1h_watchdog());
			    				
			    				if (getLocal_1h_watchdog() > 0) {
			    					request_measurement_data = true;
			    				}
			    				else {
			    					close_ComputeEngine_Runnable = true;
			    				}
			    				
			    				if (sensor.getNumberOfMeasurements() == (TCPserver.getMeasurements_limit() - 1) ) {
			    			        // update 24h local watchdog to ensure that its reading has current value
			    			        setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				}
		    				}
		    				else if(getLocal_24h_watchdog() < (get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, getWatchdog_thresholds_array())) ) {
		    					
		    					// adjust Local_24h_watchdogs based on its time left to expiration when ClientMessage_ACK has been received
		    					setLocal_24h_watchdog(_24h_Watchdog_close_to_expire(getLocal_24h_watchdog(), getLocal_watchdog_scale_factor()));
		    					
		    					setDelay(get_delays(Delays.LOW, getDelays_array()));
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog global: \t" +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog global: \t" +  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog with delay for ServerMessage_Request_MeasurementHistory (Local_24h_watchdog): \t" + getLocal_24h_watchdog());
			    				
			    				if (getLocal_24h_watchdog() > 0) {
			    					request_measurement_history = true;
			    				}
			    				else {
			    					close_ComputeEngine_Runnable = true;
			    				}
			    				
		    				}
		    				else {
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_ACK received when neither 1h_watchdog: " + getLocal_1h_watchdog() + " [s] nor 24h_watchdog: " + getLocal_24h_watchdog() + " [s] is close to expire");
	
		    					// indicate that ComputeEngine_Runnable should be closed
		    					// ClientMessage_ACK has been sent for the confirmation purposes, hence there are no further messages expected
		    					close_ComputeEngine_Runnable = true;
		    				}
		    			}
		    			else if (receivedMessage instanceof ClientMessage_SensorInfo && sensor != null) {
		    				
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_SensorInfo message from sensor: " + sensor.getSensorID() + " has been received.");
		    				SensorImpl received_sensor = ((ClientMessage_SensorInfo) receivedMessage).getSensor();
		    				
		    				if ((sensor.getCoordinates().equals(received_sensor.getCoordinates())) &&
		    					(sensor.getSoftwareImageID().equals(received_sensor.getSoftwareImageID())) &&
		    					(sensor.getSensorState().equals(received_sensor.getSensorState())) &&
		    					(sensor.getLocal_watchdog_scale_factor() == (received_sensor.getLocal_watchdog_scale_factor()))){
		    					
		    					// received sensor info is up to date
		    					// send go to operational by updating the sensor state
		    					
		    					sensor.setSensorState(SensorState.OPERATIONAL);
		    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
		    					
		    					
		    					if (received_sensor.getSensorState() == SensorState.PRE_OPERATIONAL) {
		    						
		    						// send ServerMessage_SensorInfoUpdate that changes state of the sensor to SensorState.OPERATIONAL and update Watchdogs after sensor reset upon receiving ClientMessage_MeasurementHistory
			    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
			    							 									   getLocal_1h_watchdog(), getLocal_24h_watchdog(),
		    																	   getLocal_watchdog_scale_factor(), getMeasurements_limit()), getOutputStream());
			    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate.");
			    					// serialize sensor instance and save it to file
			    					getProcessing_engine().saveSensorInfo(sensor, "gotoOPERATIONALafterRESET");
			    					
			    					// enable 1h watchdog that has been disabled during 24h watchdog processing
			    					if (!Global_1h_Watchdog.getInstance().getEnabled()){
			    						Global_1h_Watchdog.getInstance().setEnabled(true);
		    						}
		    					}
		    					else if (received_sensor.getSensorState() == SensorState.MAINTENANCE) {
		    						// send ServerMessage_SensorInfoUpdate that changes state of the sensor to SensorState.OPERATIONAL and update Watchdogs after sensor reset upon being successfully initialized
		    						sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    																	   Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 
	    																	       getLocal_watchdog_scale_factor(), getMeasurements_limit()), getOutputStream());
		    						System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate.");
		    						// serialize sensor instance and save it to file
			    					getProcessing_engine().saveSensorInfo(sensor, "gotoOPERATIONALafterCONFIGURATION");
		    					}
		    					else {
		    						// send ServerMessage_SensorInfoUpdate that confirms state of the sensor, which is SensorState.OPERATIONAL and update Watchdogs
		    						// after 1h Watchdog is close to expiration and the Measurement Data Request is going to be send 
		    						sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    																	   Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 
		    																	   getLocal_watchdog_scale_factor(), getMeasurements_limit()), getOutputStream());
		    						System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate.");
		    						// serialize sensor instance and save it to file
			    					getProcessing_engine().saveSensorInfo(sensor, "stayinOPERATIONAL");
		    					}
		        				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] send go to OPERATIONAL to sensor: " + sensor.getSensorID());
		        				//setComputeEngine_Runnable_running(false);
		        				
		    				}
		    				else {
		    					// received sensor info is out of date
		    					// send new settings and force the sensor to reset
		    					
		    					sensor.setSensorState(SensorState.MAINTENANCE);
		    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
		    					
		    					// serialize sensor instance and save it to file
		    					getProcessing_engine().saveSensorInfo(sensor, "gotoMAINTENANCEafterINITIALIZATION");

		    					// send ServerMessage_SensorInfoUpdate that changes triggers the sensor to reset, then the sensor should send ClientMessage_BootUp
		    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    							 									   Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 
		    							 									   getLocal_watchdog_scale_factor(), getMeasurements_limit()), getOutputStream());
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_SensorInfo with ServerMessage_SensorInfoUpdate.");
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] send new setting (go to MAINTENANCE) to sensor: " + sensor.getSensorID());
		    				}
		    			}
	    				else if (receivedMessage instanceof ClientMessage_MeasurementData && sensor != null) {
	    					
	    					sensor.addMeasurement(((ClientMessage_MeasurementData) receivedMessage).getMeasurementData().getPm25(),
	    									   	((ClientMessage_MeasurementData) receivedMessage).getMeasurementData().getPm10(),
	    										((ClientMessage_MeasurementData) receivedMessage).getMeasurementData().getHumidity(),
	    										((ClientMessage_MeasurementData) receivedMessage).getMeasurementData().getTemperature(),
	    										((ClientMessage_MeasurementData) receivedMessage).getMeasurementData().getPressure());
	    							
	    					MeasurementData mes_data = sensor.readLastMeasurementData();
	    					
	    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
	    					
	    					// serialize measurement data instance and save it to file
	    					getProcessing_engine().saveMeasurementDataInfo(sensor, mes_data);
	    					
	    					// feed Local 1hWatchdog
	    					if(getLocal_1h_watchdog() < Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor()) {
	    						setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() + getLocal_1h_watchdog());
	    						System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Local_1h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_1h_watchdog());
		        				
	    					}

	    					// send ServerMessage_ACK
	        				sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID(),
									getLocal_1h_watchdog(), getLocal_24h_watchdog()), getOutputStream());
	        				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_MeasurementData with ServerMessage_ACK.");
	        				
	        				//indicate to the TCPserver that the 1h watchdog for this sensor has been kicked
	        				set_1hWatchog_Timestamp_tableID_value(true, receivedMessage.getSensorID() - 1);
	        				
	        				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementData from sensor: " + sensor.getSensorID() + " has been received.");
	        				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementData message has the following timestamp: " + receivedMessage.getTimestamp());
	        	
	    				}
	    				else if (receivedMessage instanceof ClientMessage_MeasurementHistory && sensor != null) {
	    					
	    					MeasurementData[] mes_hist = ((ClientMessage_MeasurementHistory) receivedMessage).getMes_history();

	    					
	    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementHistory from sensor: " + sensor.getSensorID() + " has been received.");
	    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementHistory message has the following timestamp: " + receivedMessage.getTimestamp());
	    					

    						// serialize measurement data instance and save it to file
        					getProcessing_engine().saveMeasurementHistoryInfo(sensor, mes_hist);
	        				
        					// reset sensor to create new MeasurementData array, set NumberOfMeasurements to 0 and set SensorState to PRE_OPERATIONAL
        					sensor.resetSensor();

	    					// feed Local 24hWatchdog
        					setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() * getMeasurements_limit());
        					/*  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() is used (as the offset argument)
        					* for extending TimeLeftBeforeExpiration in the next measurement history iteration by adding the remaining time to expiration from the current 24h Watchdog iteration
	    					* setLocal_24h_watchdog( Global_24h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() +  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
	    					*/
	    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Local_24h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_24h_watchdog());
	    					
	    					//indicate to the TCPserver that the 24h watchdog for this sensor has been kicked
	        				set_24hWatchog_Timestamp_tableID_value(true, receivedMessage.getSensorID() - 1);

        					// send ServerMessage_SensorInfoUpdate that causes the server to reset and reconfigure
	    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
	    																   getLocal_1h_watchdog(), getLocal_24h_watchdog(), 
									   									   getLocal_watchdog_scale_factor(), getMeasurements_limit()), getOutputStream());
	    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_MeasurementHistory with ServerMessage_SensorInfoUpdate.");
	    					
	    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
	    				}
		    		}
					processingDelay(getDelay());
    			
					if ( (getLocal_1h_watchdog() <= (get_watchdog_threshold(Watchdog_Thresholds.LOWEST, getWatchdog_thresholds_array()))) && request_measurement_data) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementData(sensor.getSensorID()), getOutputStream());
	    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_ACK with ServerMessage_Request_MeasurementData.");
	    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Request_MeasurementData is sent to sensor ID: " + sensor.getSensorID());
	    				request_measurement_data = false;
		    		}
		    		else if ( (getLocal_24h_watchdog() <= (get_watchdog_threshold(Watchdog_Thresholds.LOWEST, getWatchdog_thresholds_array()))) && request_measurement_history)  {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementHistory(sensor.getSensorID()), getOutputStream());
	    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] responds to ClientMessage_ACK with ServerMessage_Request_MeasurementHistory.");
	    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Request_MeasurementHistory is sent to sensor ID: " + sensor.getSensorID());
	    				request_measurement_history = false;
		    		}
		    		else if (close_ComputeEngine_Runnable) {
		    			setComputeEngine_Runnable_running(false);
		    			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] does not respond to ClientMessage_ACK.");
		    		}
				}
    			else {
    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] TCP connection with sensor: " + sensor.getSensorID() + " is being closed");
    				break;
    			}
    		}
		} catch (IOException IOex) {
        	System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Error: when attempted to read Object from inputStream or write Object to outputStream on the server side");
        	IOex.printStackTrace();
        } catch (ClassNotFoundException CNFex) {
    		System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Error: when attempted to servialize/deserialize objects on the server side");
    		CNFex.printStackTrace();
		} finally {
			if(sensor != null) {
				// update watchdogs based on 1hWatchog_timestamp_table and 24hWatchog_timestamp_table one all sensors have finished their TCP connection
				update_watchgods_after_TCP_connection(get_1hWatchog_timestamp_table().get(), get_24hWatchog_timestamp_table().get(), sensor);	
			}
        	try {
        		processingDelay(get_delays(Delays.HIGHEST, getDelays_array()));
        		closeOutStream();
				closeInStream();
			} catch (IOException IOex) {
			    System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Error: when attempted to close InputStreamReader inputStream on the server side");
			    IOex.printStackTrace();
			}
        }
    }
    
	 /***********************************************************************************************************
	 * Method Name: 				private double _1h_Watchdog_close_to_expire()
	 * Description: 				modifies Local 1h watchdog that triggers particular messages to be sent via TCP connection based on  Global 1h watchdog time left to expiration
	 * Affected external variables: Global_24h_Watchdog.isPaused
	 * Returned value				local_1h_watchdog
	 * Called internal functions: 	processingDelay()
	 ***********************************************************************************************************/
	protected double _1h_Watchdog_close_to_expire(double _1h_watchdog, double watchdog_scale_factor, int sensor_number_of_measurements ) {
		
		// determine duration of an additional delay that is used for watchdogs synchronization
		double global_delay_factor = 0.25;
		double local_delay_factor = 0.5;
		//Global_1h_Watchdog.getInstance().setEnabled(false);
		if ( sensor_number_of_measurements == (TCPserver.getMeasurements_limit() - 1) ){
			Global_24h_Watchdog.getInstance().setEnabled(false);
		}
		
		if(_1h_watchdog > (get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, getWatchdog_thresholds_array()))) {
			// this delay is intended to synchronize the measurements from all sensor if their Local_1h_watchdogs have different values that are far from the threshold
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _1h_watchdog * global_delay_factor);
			_1h_watchdog = (_1h_watchdog - (_1h_watchdog * local_delay_factor));
		}
		// do not enter the additional delay if 1h watchdog value is lower that the lowest threshold and sensor_number_of_measurements equals 23
		else if ( (_1h_watchdog > get_watchdog_threshold(Watchdog_Thresholds.LOWEST, getWatchdog_thresholds_array())) && (sensor_number_of_measurements != (TCPserver.getMeasurements_limit() - 1)) ){
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _1h_watchdog * global_delay_factor);
		}
		
		// decrease _1h_watchdog regardless of the fact if it has been already decreased
		_1h_watchdog = (_1h_watchdog - (_1h_watchdog * local_delay_factor));
		
		if ( sensor_number_of_measurements == (TCPserver.getMeasurements_limit() - 1) ) {
			Global_24h_Watchdog.getInstance().setEnabled(true);
		}
		
		System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] _1h_watchdog equals : " + _1h_watchdog + " when leaving _1h_Watchdog_close_to_expire()");
		System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Global_1h_Watchdog equals : " + Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " when leaving _1h_Watchdog_close_to_expire()");
		//Global_1h_Watchdog.getInstance().setEnabled(true);
		return _1h_watchdog;
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private double _24h_Watchdog_close_to_expire()
	 * Description: 				modifies both Local and Global 24h watchdog that triggers particular messages to be sent via TCP connection based on Global 24h watchdog time left to expiration
	 * Affected external variables: Global_1h_Watchdog.isPaused
	 * Returned value				local_24h_watchdog
	 * Called internal functions: 	processingDelay()
	 ***********************************************************************************************************/
	protected double _24h_Watchdog_close_to_expire(double _24h_watchdog, double watchdog_scale_factor ) {
		
		// disable 1h watchdog - it will be enabled again once the sensor responses after the reset caused by reaching the measurement history limit
		Global_1h_Watchdog.getInstance().setEnabled(false);
		
		// determine duration of an additional delay that is used for watchdogs synchronization
		double delay_factor = 0.5;

		if( (_24h_watchdog < get_watchdog_threshold(Watchdog_Thresholds.LOWEST, getWatchdog_thresholds_array())) && (Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > get_watchdog_threshold(Watchdog_Thresholds.LOWEST, getWatchdog_thresholds_array())) ){
			// DO NOTHING - 24h watchdog does not require any changes
		}
		else if( (_24h_watchdog < get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, getWatchdog_thresholds_array())) && (_24h_watchdog > get_watchdog_threshold(Watchdog_Thresholds.LOWEST, getWatchdog_thresholds_array())) ) {
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _24h_watchdog * delay_factor);
		}
		else if ( (_24h_watchdog < get_watchdog_threshold(Watchdog_Thresholds.HIGH, getWatchdog_thresholds_array())) && (_24h_watchdog > get_watchdog_threshold(Watchdog_Thresholds.MEDIUM, getWatchdog_thresholds_array())) ) {
			delay_factor = 0.7;
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _24h_watchdog * delay_factor);
			//_24h_watchdog = (_24h_watchdog - (_24h_watchdog * delay_factor));
		}
		else if ( (_24h_watchdog < get_watchdog_threshold(Watchdog_Thresholds.HIGHEST, getWatchdog_thresholds_array())) && (_24h_watchdog > get_watchdog_threshold(Watchdog_Thresholds.HIGH, getWatchdog_thresholds_array())) ) {
			delay_factor = 0.9;
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _24h_watchdog * delay_factor);
			//_24h_watchdog = (_24h_watchdog - (_24h_watchdog * delay_factor));
		}
		
		_24h_watchdog = Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		
		return _24h_watchdog;
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private void update_watchgods_after_TCP_connection()
	 * Description: 				updates both 1h and 24h thresholds after execution of the runnable state machine
	 * Affected external variables: Global_1h_Watchdog.millisecondsLeftUntilExpiration, Global_24h_Watchdog.millisecondsLeftUntilExpiration, TCPserver.computing_time,
	 								TCPserver._1hWatchog_timestamp_table, TCPserver._24hWatchog_timestamp_table
	 * Called internal functions:   processingDelay()
	 * Called external functions: 	Global_1h_Watchdog.feed(), Global_24h_Watchdog.feed(), Global_1h_Watchdog.getInstance().getTimeFromLastFeed(),
	  								Global_24h_Watchdog.getInstance().getTimeFromLastFeed(), TCPserver.areAllTrue(), TCPserver.set_1hWatchog_Allfalse()
	 ***********************************************************************************************************/
	protected synchronized void update_watchgods_after_TCP_connection(boolean[] _1hWatchog_timestamp_table, boolean[] _24hWatchog_timestamp_table, SensorImpl sensor) {
	
		// time_offset is a local variable that measures 1h_Watchdog time left to expiration and adds this measurement to 1h_Watchdog or 24h_Watchdog once the watchdogs are kicked
		// Updating the watchdogs with this time_offset is required to send messages in defined time intervals
		// Otherwise the messages will be send with higher frequency, since the watchdogs are being kicked a certain time before they are about to expire
		double time_left_before_1h_watchdog_kicked = 0;
		double time_left_before_24h_watchdog_kicked = 0;
		// time_messages_processing is a local variable that measures how long it takes to send all messages via TCP connection
		// this variable is used for preventing watchdogs from being kicked multiple times
		double time_messages_processing = 0;
		
		// check all local 1h watchdogs - if all have been updated - kick the 1h watchdog
		if( (areAllTrue(_1hWatchog_timestamp_table)) && (!isIDTrue(_24hWatchog_timestamp_table, sensor.getSensorID())) ) {
			
			time_messages_processing = TCPserver.getComputing_time() - Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] [copmute time] it took: "+time_messages_processing+ " [s] to process all 1h watchog triggered messages (time_messages_processing)");
			
			// 1h Wathdog is kicked here for all measurement data iterations with an exception for the measurement history iteration
			// it is to make sure that 1h Wathdog will be kicked once the last instance of sensor sends its measurement history
			if( Global_1h_Watchdog.getInstance().getTimeFromLastFeed() > time_messages_processing) {
								
				// set time_offset - 1h_Watchdog time left to expiration prior to have been kicked
				time_left_before_1h_watchdog_kicked =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
				
				// set computing_time - duration of compute engine execution (subtract the current 1h watchdog time left to expiration from the time that has been measured once Compute engine has started)
				TCPserver.setComputing_time(TCPserver.getComputing_time() - time_left_before_1h_watchdog_kicked);
				
				// kick 1h Global Watchdog
				Global_1h_Watchdog.getInstance().feed();
				
				//  once 1h Watchdog is kicked - set local 1h watchdog flags in the 1hWatchog_timestamp_table array to FALSE
				set_1hWatchog_Allfalse();
				
				// in case this loop has been entered multiple times (thread concurrency - indicated by negative value of TCPserver.getComputing_time()) - prevent from updating the 1h Watchdog value by time offset multiple times
				if(TCPserver.getComputing_time() > 0) {
					
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked");
					
					if(time_left_before_1h_watchdog_kicked < (Global_1h_Watchdog.getInstance().getExpiration() * local_watchdog_scale_factor)) {
						// update 1h Watchdog by time_offset - time left to expiration when 1h watchdog has been kicked
						Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration((double)(Global_1h_Watchdog.getInstance().getExpiration() * local_watchdog_scale_factor) + 0.75 * time_left_before_1h_watchdog_kicked);
						System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked when it has: "+time_left_before_1h_watchdog_kicked+ " [s] left to expire");
					}
					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] [copmute time] it took: "+TCPserver.getComputing_time()+ " [s] to execute TCP connection");
				}
				
			}
			// check all local 24h watchdogs - if they haven't been updated - do not kick 1h watchdog
			else if (!areAllTrue(_24hWatchog_timestamp_table)) {
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has not been kicked");
			}
			// all local 24h watchdogs  have been updated 
			else {
				// DO NOTHING
				// 1h watchdog will be kicked in the next if statement that handles 24h watchdog
			}
		}
		// check all local 24h watchdogs - if all have been updated - kick the 24h watchdog & 1h watchdog
		else if(areAllTrue(_24hWatchog_timestamp_table) && (Global_24h_Watchdog.getInstance().getTimeFromLastFeed() > time_messages_processing)) {
			
			// set time_offset - 1h_Watchdog time left to expiration prior to have been kicked
			time_left_before_1h_watchdog_kicked =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
			time_left_before_24h_watchdog_kicked =  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();

			// set computing_time - duration of compute engine execution (subtract the current 24h watchdog time left to expiration from the time that has been measured once Compute engine has started)
			TCPserver.setComputing_time(TCPserver.getComputing_time() - time_left_before_1h_watchdog_kicked);
			
			// kick 1h Global Watchdog
			Global_1h_Watchdog.getInstance().feed();
			
			//  once 1h Watchdog is kicked - set local 1h watchdog flags in the 1hWatchog_timestamp_table array to FALSE
			set_1hWatchog_Allfalse();
			
			// in case this loop has been entered multiple times (thread concurrency - indicated by negative value of TCPserver.getComputing_time()) - prevent from updating the 1h Watchdog value by time offset multiple times
			if(TCPserver.getComputing_time() > 0) {	
				
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked");
				
				if(time_left_before_1h_watchdog_kicked < (Global_1h_Watchdog.getInstance().getExpiration() * local_watchdog_scale_factor)) {
					// update 1h Watchdog by time_offset - time left to expiration when 1h watchdog has been kicked
					Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration((double)(Global_1h_Watchdog.getInstance().getExpiration() * local_watchdog_scale_factor) + 0.75 * time_left_before_1h_watchdog_kicked);
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked when it has: "+time_left_before_1h_watchdog_kicked+ " [s] left to expire");
				}
			}
			
			// kick 24h Global Watchdog			
			Global_24h_Watchdog.getInstance().feed();
			
			// in case this loop has been entered multiple times (thread concurrency - indicated by negative value of TCPserver.getComputing_time()) - prevent from updating the 24h Watchdog value by time offset multiple times
			if(TCPserver.getComputing_time() > 0) {
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24hWatchdog has been kicked");
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24hWatchdog has been kicked when it has: "+time_left_before_24h_watchdog_kicked+ " [s] left to expire");
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] [copmute time] it took: "+TCPserver.getComputing_time()+ " [s] to execute TCP connection");
			}	
		}
		else {
			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Neither 1hWatchdog nor 24hWatchdog have not been kicked");
			if( Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() >  Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() ) {
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] there are " + (TCPserver.getMeasurements_limit() - sensor.getNumberOfMeasurements()) +" measurement(s) more remaining to kick 24hWatchdog");
			}
		}
		
		// if measurement history has been received for this sensor instance - delete measurement datas 
		if (isIDTrue(get_24hWatchog_timestamp_table().get(), sensor.getSensorID()) ) {
			TCPserver.getProcessing_engine().deleteMeasurementDataInfo(sensor);
			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] all Measurements Datas for sensor ID: "+sensor.getSensorID()+" have been deleted");
		}

		// if 24h Watchdog has been kicked - set local 24h watchdog flags in the 24hWatchog_timestamp_table array to FALSE
		if(areAllTrue(get_24hWatchog_timestamp_table().get())) {
			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] set local 24h watchdog flags in the 24hWatchog_timestamp_table array to FALSE");
			// delay is to make sure that the race condition for variables that define the ComputeEngine_Runnable closing activities between parallel threads will not occur
			processingDelay(get_delays(Delays.HIGHEST, getDelays_array()));
			set_24hWatchog_Allfalse();
		}
	}
	
    /***********************************************************************************************************
	 * Method Name: 				static void processingDelay()
	 * Description: 				stops thread execution for a particular time in [ms]
	 * Affected internal variables: Thread
	 * Exceptions handled: 			InterruptedException
	 ***********************************************************************************************************/
	static void processingDelay(double msec) {
	    try {
	        Thread.sleep( (int) msec);
	    } catch (InterruptedException ex) {
	        
	    }
    }
    
    /***********************************************************************************************************
	 * Method Name: 				public void closeOutStream()
	 * Description: 				closes object output stream
	 * Affected internal variables: outputStream
	 * Exceptions thrown: 			IOException, IllegalArgumentException
	 ***********************************************************************************************************/
	public void closeOutStream() throws IOException {
		if (outputStream != null) {
			outputStream.close();
		}
		else {
			throw new IllegalArgumentException();
		}   
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void closeInStream()
	 * Description: 				closes object input stream
	 * Affected internal variables: inputStream
	 * Exceptions thrown: 			IOException, IllegalArgumentException
	 ***********************************************************************************************************/
	public void closeInStream() throws IOException {
		if (inputStream != null) {
			inputStream.close();
		}
		else {
			throw new IllegalArgumentException();
		}   
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private int[] set_delays_array()
	 * Description: 				sets delays that will be used in the runnable state machine based on watchdog_scale_factor
	 * Returned value				delays_array
	 ***********************************************************************************************************/
	protected int[] set_delays_array(double watchdog_scale_factor, int array_size) {
		
		int[] temp_delays_array =  new int[array_size];
		
		if (watchdog_scale_factor != 1.0 ) {
			temp_delays_array[0] = (int)(100 * watchdog_scale_factor);
			temp_delays_array[1] = (int)(1000 * watchdog_scale_factor);
			temp_delays_array[2] = (int)(10000 * watchdog_scale_factor);
			temp_delays_array[3] = (int)(100000 * watchdog_scale_factor);
		}
		else {
			temp_delays_array[0] = 100;
			temp_delays_array[1] = 1000;
			temp_delays_array[2] = 10000;
			temp_delays_array[3] = 100000;
		}
		
		return temp_delays_array;	
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private double[] set_watchdog_thresholds_array()
	 * Description: 				sets watchsdog thresholds that will be used in the runnable state machine based on watchdog_scale_factor
	 * Returned value	 			watchdog_thresholds_array
	 ***********************************************************************************************************/
	protected double[] set_watchdog_thresholds_array(double watchdog_scale_factor, int array_size) {
		
		double[] temp_watchdog_thresholds_array =  new double[array_size];
		
		if (watchdog_scale_factor != 1.0 ) {
			temp_watchdog_thresholds_array[0] = 100 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[1] = 150 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[2] = 300 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[3] = 900 * watchdog_scale_factor;
		}
		else {
			temp_watchdog_thresholds_array[0] = 100;
			temp_watchdog_thresholds_array[1] = 150;
			temp_watchdog_thresholds_array[2] = 300;
			temp_watchdog_thresholds_array[3] = 900;
		}
		
		return temp_watchdog_thresholds_array;	
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private double get_watchdog_threshold()
	 * Description: 				retrieves watchsdog threshold based on input Watchdog_Thresholds enumeration
	 * Returned value	 			watchdog_threshold
  	 ***********************************************************************************************************/
	protected double get_watchdog_threshold(Watchdog_Thresholds THRESHOLD, double[] watchdog_thresholds_array) {
		
		double watchdog_threshold = 0;
		
		switch(THRESHOLD){
			case LOWEST: watchdog_threshold = watchdog_thresholds_array[0];
				break;
			case MEDIUM: watchdog_threshold = watchdog_thresholds_array[1];
				break;
			case HIGH: watchdog_threshold = watchdog_thresholds_array[2];
				break;
			case HIGHEST: watchdog_threshold = watchdog_thresholds_array[3];
				break;
			default:
				break;
		}

		return watchdog_threshold;
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private int get_delays()
	 * Description: 				retrieves delay based on input Delays enumeration
	 * Returned value	 			delay
	 ***********************************************************************************************************/
	protected int get_delays(Delays DELAY, int[] delays_array) {
		
		int delay = 0;
		
		switch(DELAY){
			case LOWEST: delay = delays_array[0];
				break;
			case LOW: delay = delays_array[1];
				break;	
			case MEDIUM: delay = delays_array[2];
				break;
			case HIGHEST: delay = delays_array[3];
				break;
			default:
				break;
		}

		return delay;
	}
	
    /***********************************************************************************************************
	 * Auxiliary piece of code
	 * Description: 				getters & setters for class attributes			
	 ***********************************************************************************************************/
	public ObjectOutputStream getOutputStream() {
		return this.outputStream;
	}

	public ObjectInputStream getInputReaderStream() {
		return this.inputStream;
	}
	
	public synchronized boolean isComputeEngine_Runnable_running() {
		return isComputeEngine_Runnable_running;
	}

	public synchronized void setComputeEngine_Runnable_running(boolean isComputeEngine_Runnable_running) {
		this.isComputeEngine_Runnable_running = isComputeEngine_Runnable_running;
	}
	
	public int getDelay() {
		return delay;
	}

	public void setDelay(double delay) {
		this.delay = (int) delay;
	}
	
	public double getLocal_1h_watchdog() {
		return local_1h_watchdog;
	}

	public void setLocal_1h_watchdog(double local_1h_watchdog) {
		this.local_1h_watchdog = local_1h_watchdog;
	}

	public double getLocal_24h_watchdog() {
		return local_24h_watchdog;
	}

	public void setLocal_24h_watchdog(double local_24h_watchdog) {
		this.local_24h_watchdog = local_24h_watchdog;
	}
	
	public double getLocal_watchdog_scale_factor() {
		return local_watchdog_scale_factor;
	}

	public void setLocal_watchdog_scale_factor(double local_watchdog_scale_factor) {
		this.local_watchdog_scale_factor = local_watchdog_scale_factor;
	}

	public double[] getWatchdog_thresholds_array() {
		return watchdog_thresholds_array;
	}

	public void setWatchdog_thresholds_array(double[] watchdog_thresholds_array) {
		this.watchdog_thresholds_array = watchdog_thresholds_array;
	}

	public int[] getDelays_array() {
		return delays_array;
	}

	public void setDelays_array(int[] delays_array) {
		this.delays_array = delays_array;
	}
	
    public void setSensor(SensorImpl sensor) {
		this.sensor = sensor;
	}

}

