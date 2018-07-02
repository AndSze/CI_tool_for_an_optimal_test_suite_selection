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
    private static double[] _24h_watchdog_array = new double[24];
    
	/***********************************************************************************************************
   	 * Auxiliary piece of code
   	 * Specific Variable Names: 	1) double local_watchdog_scale_factor
	 								2) int[] delays_array
	 								3) final int delays_array_size
	 								4) double[] watchdog_thresholds_array
	 								5) final int watchdog_thresholds_array_size
	 * Description: 				Interfaces for the testing purposes - to parameterize times dependencies
	 ***********************************************************************************************************/
    private double local_watchdog_scale_factor = 1.0;
    private int[] delays_array;
    private final int delays_array_size = 4;
    private double[] watchdog_thresholds_array;
    private final int watchdog_thresholds_array_size = 4;
   
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
        
        // set 1h local watchdog to 1h global watchdog time left before expiration
        setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        
        // set 24h local watchdog to 24h global watchdog time left before expiration
        setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        
        // set delays in miliseconds that are determined based on watchdog_scale_factor
        setDelays_array(set_delays_array(global_watchdog_scale_factor, delays_array_size));
        
        // set watchdogs thresholds in secongs that are determined based on watchdog_scale_factor
        setWatchdog_thresholds_array(set_watchdog_thresholds_array(global_watchdog_scale_factor, watchdog_thresholds_array_size));
        
        System.out.println("[Compute engine Runnable] Multithreaded Server Service has been started");
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private void sendMessage()
	 * Description: 				writes object that has to inherit from Message_Interface class to object output stream
	 * Affected internal variables: outputStream
	 * Affected external variables: Message_Interface
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	private void sendMessage(Message_Interface message) throws IOException {
		
        // it sends the message via output stream
		getOutputStream().writeObject(message);
		processingDelay(get_delays(Delays.LOWEST));
	}
	
	/***********************************************************************************************************
	 * Method Name: 				public void run()
	 * Description: 				runnable state machine for massages sent to sensors based on received messages via TCP connection and watchdogs time left to expiration
	 * Affected internal variables: inputStream, outputStream, delay, local_1h_watchdog, local_24h_watchdog
	 * Affected external variables: SensorImpl, TCPserver._1hWatchog_timestamp_table, TCPserver._24hWatchog_timestamp_table, TCPserver.processing_engine, TCPserver.Server_Sensors_LIST,
	 								TCPserver.MeasurementHistory_LIST, TCPserver.MeasurementData_LIST
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
    			
    			setDelay(get_delays(Delays.LOW));
    			if(isComputeEngine_Runnable_running()) {
    				
					if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null) {
		    			
	    				sensor = getProcessing_engine().searchInServerSensorList(receivedMessage.getSensorID());
						
		    			if (receivedMessage instanceof ClientMessage_BootUp) {
		    				
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] BootUp message from sensor: " + sensor.getSensorID() + " has been received.");
		    				sendMessage(new ServerMessage_SensorInfoQuerry(receivedMessage.getSensorID()));
		    			}
		    			else if (receivedMessage instanceof ClientMessage_ACK) {
		    				
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] received ClientMessage_ACK from sensor: " + sensor.getSensorID() + " has been received.");
		    				if (getLocal_1h_watchdog() < (get_watchdog_threshold(Watchdog_Thresholds.HIGH)) ) {
		    					
		    					// adjust Local_1h_watchdogs based on its time left to expiration when ClientMessage_ACK has been received
			    				setLocal_1h_watchdog(_1h_Watchdog_close_to_expire(getLocal_1h_watchdog(), getLocal_watchdog_scale_factor()));
		    					
			    				setDelay(get_delays(Delays.LOW));
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog global: \t" +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog global: \t" +  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog with delay for ServerMessage_Request_MeasurementData (Local_1h_watchdog): \t" + getLocal_1h_watchdog());
			    				
			    				if (getLocal_1h_watchdog() > 0) {
			    					request_measurement_data = true;
			    				}
			    				
			    				if (sensor.getNumberOfMeasurements() == 23) {
			    			        // update 24h local watchdog to ensure that its reading has current value
			    			        setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				}
		    				}
		    				else if(getLocal_24h_watchdog() < (get_watchdog_threshold(Watchdog_Thresholds.HIGHEST)) ) {
		    					
		    					// adjust Local_24h_watchdogs based on its time left to expiration when ClientMessage_ACK has been received
		    					setLocal_24h_watchdog(_24h_Watchdog_close_to_expire(getLocal_24h_watchdog(), getLocal_watchdog_scale_factor()));
		    					
		    					setDelay(get_delays(Delays.LOW));
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog global: \t" +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog global: \t" +  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog with delay for ServerMessage_Request_MeasurementHistory (Local_24h_watchdog): \t" + getLocal_24h_watchdog());
			    				
			    				if (getLocal_1h_watchdog() > 0) {
			    					request_measurement_history = true;
			    				}
		    				}
		    				else {
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_ACK received when neither 1h_watchdog: " + getLocal_1h_watchdog() + " [s] nor 24h_watchdog: " + getLocal_24h_watchdog() + " [s] is close to expire");
	
		    					// indicate that ComputeEngine_Runnable should be closed
		    					// ClientMessage_ACK has been sent for the confirmation purposes, hence there are no further messages expected
		    					close_ComputeEngine_Runnable = true;
		    				}
		    			}
		    			else if (receivedMessage instanceof ClientMessage_SensorInfo) {
		    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_SensorInfo message from sensor: " + sensor.getSensorID() + " has been received.");
		    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_SensorInfo message has the following timestamp: " + receivedMessage.getTimestamp());
		    				SensorImpl received_sensor = ((ClientMessage_SensorInfo) receivedMessage).getSensor();
		    				
		    				/*
		    				System.out.println("sensor.getCoordinates(): "+ sensor.getCoordinates() + "\t received_sensor.getCoordinates(): "+ received_sensor.getCoordinates());
		    				System.out.println("sensor.getSoftwareImageID(): "+ sensor.getSoftwareImageID()+ "\t received_sensor.getSoftwareImageID(): "+ received_sensor.getSoftwareImageID());
		    				System.out.println("sensor.getSensorState(): "+ sensor.getSensorState()+ "\t received_sensor.getSensorState(): "+ received_sensor.getSensorState());
		    				*/
		    				
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
		    																	   getLocal_watchdog_scale_factor(), getMeasurements_limit()));
			    					// serialize sensor instance and save it to file
			    					getProcessing_engine().saveSensorInfo(sensor, "gotoOPERATIONALafterRESET");
		    					}
		    					else if (received_sensor.getSensorState() == SensorState.MAINTENANCE) {
		    						// send ServerMessage_SensorInfoUpdate that changes state of the sensor to SensorState.OPERATIONAL and update Watchdogs after sensor reset upon being successfully initialized
		    						sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    																	   Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 
	    																	       getLocal_watchdog_scale_factor(), getMeasurements_limit()));
		    						// serialize sensor instance and save it to file
			    					getProcessing_engine().saveSensorInfo(sensor, "gotoOPERATIONALafterCONFIGURATION");
		    					}
		    					else {
		    						// send ServerMessage_SensorInfoUpdate that confirms state of the sensor, which is SensorState.OPERATIONAL and update Watchdogs
		    						// after 1h Watchdog is close to expiration and the Measurement Data Request is going to be send 
		    						sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    																	   Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(), 
		    																	   getLocal_watchdog_scale_factor(), getMeasurements_limit()));
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
		    							 									   getLocal_watchdog_scale_factor(), getMeasurements_limit()));
		    					
		    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] send new setting (go to MAINTENANCE) to sensor: " + sensor.getSensorID());
		    				}
		    			}
	    				else if (receivedMessage instanceof ClientMessage_MeasurementData) {
	    					
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
	    					setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() );
        					/*  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() is used (as the offset argument)
        					* for extending TimeLeftBeforeExpiration in the next measurement data iteration by adding the remaining time to expiration from the current 1h Watchdog iteration
	    					* setLocal_1h_watchdog( Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
	    					*/
	    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Local_1h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_1h_watchdog());
	        				
	    					// send ServerMessage_ACK
	        				sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID(),
									getLocal_1h_watchdog(), getLocal_24h_watchdog()));
	        				
	        				//indicate to the TCPserver that the 1h watchdog for this sensor has been kicked
	        				set_1hWatchog_Timestamp_tableID_value(true, receivedMessage.getSensorID() - 1);
	        				
	        				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementData from sensor: " + sensor.getSensorID() + " has been received.");
	        				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementData message has the following timestamp: " + receivedMessage.getTimestamp());
	        	
	    				}
	    				else if (receivedMessage instanceof ClientMessage_MeasurementHistory) {
	    					
	    					MeasurementData[] mes_hist = ((ClientMessage_MeasurementHistory) receivedMessage).getMes_history();

	    					
	    					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementHistory from sensor: " + sensor.getSensorID() + " has been received.");
	    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementHistory message has the following timestamp: " + receivedMessage.getTimestamp());
	    					

    						// serialize measurement data instance and save it to file
        					getProcessing_engine().saveMeasurementHistoryInfo(sensor, mes_hist);
	        				
        					// reset sensor to create new MeasurementData array, set NumberOfMeasurements to 0 and set SensorState to PRE_OPERATIONAL
        					sensor.resetSensor();

	    					// feed Local 24hWatchdog
        					setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor());
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
									   									   getLocal_watchdog_scale_factor(), getMeasurements_limit()));
	    					
	    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
	    				}
		    		}
					processingDelay(getDelay());
					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Processing Delay is set to: " + getDelay());
    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog local: \t" + getLocal_1h_watchdog());
    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog local: \t" + getLocal_24h_watchdog());
    			
					if ( (getLocal_1h_watchdog() <= (get_watchdog_threshold(Watchdog_Thresholds.LOWEST))) && request_measurement_data) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementData(sensor.getSensorID()));
	    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Request_MeasurementData is being send to sensor ID: " + sensor.getSensorID());
	    				request_measurement_data = false;
		    		}
		    		else if ( (getLocal_24h_watchdog() <= (get_watchdog_threshold(Watchdog_Thresholds.LOWEST))) && request_measurement_history) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementHistory(sensor.getSensorID()));
	    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Request_MeasurementHistory is being send to sensor ID: " + sensor.getSensorID());
	    				request_measurement_history = false;
		    		}
		    		else if (close_ComputeEngine_Runnable) {
		    			setComputeEngine_Runnable_running(false);
		    		}
				}
    			else {
    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] for sensor ID: " + sensor.getSensorID() + " is being closed");
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
			// update watchdogs based on 1hWatchog_timestamp_table and 24hWatchog_timestamp_table one all sensors have finished their TCP connection
			update_watchgods_after_TCP_connection(get_1hWatchog_timestamp_table().get(), get_24hWatchog_timestamp_table().get(), sensor);	
			if (sensor.getNumberOfMeasurements() != 0) {
				_24h_watchdog_array[sensor.getNumberOfMeasurements()-1] = Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
			}
			else {
				_24h_watchdog_array[0] = Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
			}
			if (sensor.getNumberOfMeasurements() == 23) {
				for(int i = 0; i< _24h_watchdog_array.length; i++) {
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] _24h_watchdog_array["+i+"] equals: " +_24h_watchdog_array[i]);
					
				}
				//TCPserver.set_ServerRunning(false);
			}
        	try {
        		processingDelay(get_delays(Delays.HIGHEST));
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
	 * Returned value				local_1h_watchdog
	 * Called internal functions: 	processingDelay()
	 ***********************************************************************************************************/
	private double _1h_Watchdog_close_to_expire(double _1h_watchdog, double watchdog_scale_factor ) {
		
		// determine duration of an additional delay that is used for watchdogs synchronization
		double delay_factor = 0.5;
		
		if(_1h_watchdog > (get_watchdog_threshold(Watchdog_Thresholds.MEDIUM))) {
			// this delay is intended to synchronize the measurements from all sensor if their Local_1h_watchdogs have different values that are far from the threshold
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _1h_watchdog * delay_factor);
			_1h_watchdog = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		}
		
		// decrease _1h_watchdog regardless of the fact if it has been already decreased
		_1h_watchdog = (_1h_watchdog - (_1h_watchdog * delay_factor));
		if (_1h_watchdog > get_watchdog_threshold(Watchdog_Thresholds.LOWEST)){
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _1h_watchdog * delay_factor);
		}
		
		return _1h_watchdog;
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private double _24h_Watchdog_close_to_expire()
	 * Description: 				modifies both Local and Global 24h watchdog that triggers particular messages to be sent via TCP connection based on  Global 24h watchdog time left to expiration
	 * Returned value				local_24h_watchdog
	 * Affected external variables: Global_1h_Watchdog.millisecondsLeftUntilExpiration, Global_24h_Watchdog.millisecondsLeftUntilExpiration
	 ***********************************************************************************************************/
	private double _24h_Watchdog_close_to_expire(double _24h_watchdog, double watchdog_scale_factor ) {
		
		// determine duration of an additional delay that is used for watchdogs synchronization
		//double delay_factor = 0.5;
		/*
		if(_24h_watchdog > (get_watchdog_threshold(Watchdog_Thresholds.MEDIUM)) ) {
			// this delay is intended to synchronize the measurements from all sensor if their Local_1h_watchdogs have different values that are far from the threshold
			processingDelay((int)seconds_to_miliseconds_conversion_factor * _24h_watchdog * delay_factor);
			_24h_watchdog = Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		}
		*/
		if( (_24h_watchdog < get_watchdog_threshold(Watchdog_Thresholds.LOWEST)) || (Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() < get_watchdog_threshold(Watchdog_Thresholds.LOWEST)) ){
			if(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() < Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration()) {
				Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + TCPserver.getComputing_time());
			}
			else {
				Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + TCPserver.getComputing_time());
			}
		}
		
		// decrease _24h_watchdog regardless of the fact if it has been already decreased
		//_24h_watchdog = (_24h_watchdog - (_24h_watchdog * delay_factor));
		//processingDelay((int)seconds_to_miliseconds_conversion_factor * _24h_watchdog * delay_factor);
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
	private void update_watchgods_after_TCP_connection(boolean[] _1hWatchog_timestamp_table, boolean[] _24hWatchog_timestamp_table, SensorImpl sensor) {
		
		// time_offset is a local variable that measures 1h_Watchdog time left to expiration and adds this measurement to 1h_Watchdog or 24h_Watchdog once the watchdogs are kicked
		// Updating the watchdogs with this time_offset is required to send messages in defined time intervals
		// Otherwise the messages will be send with higher frequency, since the watchdogs are being kicked a certain time before they are about to expire
		double time_left_before_1h_watchdog_kicked = 0;
		double time_left_before_24h_watchdog_kicked = 0;
		
		// time_messages_processing is a local variable that measures how long it takes to send all messages via TCP connection
		// this variable is used for preventing watchdogs from being kicked multiple times
		double time_messages_processing = 0;
		time_messages_processing = TCPserver.getComputing_time() - Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
		
		// check all local 1h watchdogs - if all have been updated - kick the 1h watchdog
		if(areAllTrue(_1hWatchog_timestamp_table) ) {
			
			// 1h Wathdog is kicked here for all measurement data iterations with an exception for the measurement history iteration
			// it is to make sure that 1h Wathdog will be kicked once the last instance of sensor sends its measurement history
			if(!isIDTrue(_24hWatchog_timestamp_table, sensor.getSensorID()) && (Global_1h_Watchdog.getInstance().getTimeFromLastFeed() > time_messages_processing)) {
				
				// set time_offset - 1h_Watchdog time left to expiration prior to have been kicked
				time_left_before_1h_watchdog_kicked =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
				
				// set computing_time - duration of compute engine execution (subtract the current 1h watchdog time left to expiration from the time that has been measured once Compute engine has started)
				
				TCPserver.setComputing_time(TCPserver.getComputing_time() - time_left_before_1h_watchdog_kicked);
				// kick 1h Global Watchdog
				Global_1h_Watchdog.getInstance().feed();
				
				// update 1h Watchdog by time_offset - time left to expiration when 1h watchdog has been kicked
				Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + time_left_before_1h_watchdog_kicked);
				
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked");
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked when it has: "+time_left_before_1h_watchdog_kicked+ " [s] left to expire");
				
				if(TCPserver.getComputing_time() > 0) {
					// update 24h Watchdog by computing_time - duration of compute engine execution
					Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + time_left_before_1h_watchdog_kicked);
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] [copmute time] it took: "+TCPserver.getComputing_time()+ " [s] to execute TCP connection");
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Global_24h_Watchdog has been updated and it equals : "+Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration()+ " [s]");
				}
				
				//  once 1h Watchdog is kicked - set local 1h watchdog flags in the 24hWatchog_timestamp_table array to FALSE
				set_1hWatchog_Allfalse();
			}
			// check all local 24h watchdogs - if they haven't been updated - do not kick 1h watchdog
			else if (!areAllTrue(_24hWatchog_timestamp_table)) {
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has not been kicked");
			}
			// all local 24h watchdogs  have been updated 
			else {
				// DO NOTHING
				// 1h watchdog will be kicked in the next if statement
			}
		}
		// check all local 24h watchdogs - if all have been updated - kick the 24h watchdog & 1h watchdog
		if(areAllTrue(_24hWatchog_timestamp_table) && (Global_24h_Watchdog.getInstance().getTimeFromLastFeed() > time_messages_processing)) {

			time_left_before_24h_watchdog_kicked =  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
			
			// set computing_time - duration of compute engine execution (subtract the current 24h watchdog time left to expiration from the time that has been measured once Compute engine has started)
			TCPserver.setComputing_time(TCPserver.getComputing_time() - time_left_before_24h_watchdog_kicked);
			
			if (Global_1h_Watchdog.getInstance().getTimeFromLastFeed() > time_messages_processing){
				
				// set time_offset - 1h_Watchdog time left to expiration prior to have been kicked
				time_left_before_1h_watchdog_kicked =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
				
				// kick 1h Global Watchdog
				Global_1h_Watchdog.getInstance().feed();
				
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked");
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked when it has: "+time_left_before_1h_watchdog_kicked+ " [s] left to expire");
				
				if(TCPserver.getComputing_time() > 0) {
					// update 1h Watchdog by subtracting TCPserver.getComputing_time() - Compute Engine execution time
					// it is required to do not exceeding time interval between 24th and 1st measurement data request
					Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() - TCPserver.getComputing_time());
				}
				
				// once 1h Watchdog is kicked - set local 1h watchdog flags in the 24hWatchog_timestamp_table array to FALSE
				set_1hWatchog_Allfalse();
			}
			
			// kick 24h Global Watchdog
			 Global_24h_Watchdog.getInstance().feed();
			 
			if(TCPserver.getComputing_time() > 0) {
				// update 24h Watchdog by subtracting TCPserver.getComputing_time() - Compute Engine execution time
				// it is required to do not exceeding time interval between 24th and 1st measurement data request
				Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() - TCPserver.getComputing_time()); 
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] [copmute time] it took: "+TCPserver.getComputing_time()+ " [s] to execute TCP connection");
			}
			
			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24hWatchdog has been kicked");				
			
		}
		else {
			System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24hWatchdog has not been kicked");
			if( Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() >  Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() ) {
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] there are " + (TCPserver.getMeasurements_limit() - sensor.getNumberOfMeasurements()) +" measurement(s) more remaining to kick 24hWatchdog");
			}
		}
		
		// if measurement history has been received for this sensor instance - delete measurement datas 
		if (isIDTrue(get_24hWatchog_timestamp_table().get(), sensor.getSensorID()) ) {
			getProcessing_engine().deleteMeasurementDataInfo(sensor);
		}
		
		// if 24h Watchdog has been kicked - set local 24h watchdog flags in the 24hWatchog_timestamp_table array to FALSE
		if(areAllTrue(get_24hWatchog_timestamp_table().get())) {
			
			// delay is to make sure that the race condition for variables that define the ComputeEngine_Runnable closing activities between parallel threads will not occur
			processingDelay(get_delays(Delays.HIGHEST));
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
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	public void closeOutStream() throws IOException {
		if (outputStream!=null) {
			outputStream.close();
		}
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void closeInStream()
	 * Description: 				closes object input stream
	 * Affected internal variables: inputStream
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	public void closeInStream() throws IOException {
		if (inputStream!=null) {
			inputStream.close();
		}
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private int[] set_delays_array()
	 * Description: 				sets delays that will be used in the runnable state machine based on watchdog_scale_factor
	 * Returned value				delays_array
	 ***********************************************************************************************************/
	private int[] set_delays_array(double watchdog_scale_factor, int array_size) {
		
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
	private double[] set_watchdog_thresholds_array(double watchdog_scale_factor, int array_size) {
		
		double[] temp_watchdog_thresholds_array =  new double[array_size];
		
		if (watchdog_scale_factor != 1.0 ) {
			temp_watchdog_thresholds_array[0] = 100 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[1] = 150 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[2] = 300 * watchdog_scale_factor;
			temp_watchdog_thresholds_array[3] = 1200 * watchdog_scale_factor;
		}
		else {
			temp_watchdog_thresholds_array[0] = 100;
			temp_watchdog_thresholds_array[1] = 150;
			temp_watchdog_thresholds_array[2] = 300;
			temp_watchdog_thresholds_array[3] = 1200;
		}
		
		return temp_watchdog_thresholds_array;	
	}
	
	/***********************************************************************************************************
	 * Method Name: 				private double get_watchdog_threshold()
	 * Description: 				retrieves watchsdog threshold based on input Watchdog_Thresholds enumeration
	 * Returned value	 			watchdog_threshold
  	 ***********************************************************************************************************/
	private double get_watchdog_threshold(Watchdog_Thresholds THRESHOLD) {
		
		double watchdog_threshold = 0;
		
		switch(THRESHOLD){
			case LOWEST: watchdog_threshold = getWatchdog_thresholds_array()[0];
				break;
			case MEDIUM: watchdog_threshold = getWatchdog_thresholds_array()[1];
				break;
			case HIGH: watchdog_threshold = getWatchdog_thresholds_array()[2];
				break;
			case HIGHEST: watchdog_threshold = getWatchdog_thresholds_array()[3];
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
	private int get_delays(Delays DELAY) {
		
		int delay = 0;
		
		switch(DELAY){
			case LOWEST: delay = getDelays_array()[0];
				break;
			case LOW: delay = getDelays_array()[1];
				break;	
			case MEDIUM: delay = getDelays_array()[2];
				break;
			case HIGHEST: delay = getDelays_array()[3];
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

}

