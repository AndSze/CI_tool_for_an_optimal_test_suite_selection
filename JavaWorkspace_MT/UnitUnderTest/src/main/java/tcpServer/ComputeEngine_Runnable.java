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

public class ComputeEngine_Runnable extends TCPserver implements Runnable {
	
    private ObjectOutputStream outputStream = null;
    private ObjectInputStream inputStream = null;
    private SensorImpl sensor = null;
    private boolean isComputeEngine_Runnable_running = false;
    private int delay = 100;
    double local_1h_watchdog = 0;
    double local_24h_watchdog = 0;
    double local_watchdog_scale_factor = 1.0;

	public ComputeEngine_Runnable(Socket clientSocket, double global_watchdog_scale_factor, boolean isComputeEngine_Runnable_running) throws IOException  {
		super();
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        
        // local watchdog factor has to be set prior to setting local watchdogs since those values need to be scaled with this factor
        setLocal_watchdog_scale_factor(global_watchdog_scale_factor);
        
        setComputeEngine_Runnable_running(isComputeEngine_Runnable_running);
        setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());

        
        //System.out.println("[Compute engine Runnable] Multithreaded Server Service has been started");
	}

	public void sendMessage(Message_Interface message) throws IOException {
		
        // it sends the message via output stream
			getOutputStream().writeObject(message);
        
	}

    public void run() {
    	
    	// flag that will indicate that rverMessage_Request_MeasurementHistory is going to be send
    	boolean close_ComputeEngine_Runnable = false;
    	double time_offset = 0;
    	boolean request_measurement_history = false;
        
    	//synchronized (Echo) {
    	Message_Interface receivedMessage = null;
    	try {
    		while(true) {
    			
    			setDelay(1000 * getLocal_watchdog_scale_factor());
    			if(isComputeEngine_Runnable_running()) {
    				
					if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null) {
		    			
	    				sensor = getProcessing_engine().searchInServerSensorList(receivedMessage.getSensorID());
						
		    			if (receivedMessage instanceof ClientMessage_BootUp) {
		    										
		    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] BootUp message from sensor: " + sensor.getSensorID() + " has been received.");
		    				////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] BootUp message has the following timestamp: " + receivedMessage.getTimestamp());
		    				// send ServerMessage_SensorInfoQuerry
		    				sendMessage(new ServerMessage_SensorInfoQuerry(receivedMessage.getSensorID()));
		    			}
		    			else if (receivedMessage instanceof ClientMessage_ACK) {
		    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] received ClientMessage_ACK from sensor: " + sensor.getSensorID() + " has been received.");
		    				////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Processing Delay set to: " + getDelay());
		    				////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog global: \t" +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    				////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog local: \t" + getLocal_1h_watchdog());
		    				if(getLocal_1h_watchdog() < (300 * getLocal_watchdog_scale_factor()) ) {
		    					if(getLocal_1h_watchdog() > (150 * getLocal_watchdog_scale_factor()) ) {
		    						// this delay is intended to synchronize the measurements from all sensor if their Local_1h_watchdogs have different values that are far from the threshold
		    						processingDelay((int)1000 * getLocal_1h_watchdog()*0.5);
		    						setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Synchronizig 1h_Watchdogs by activating an additional delay, 1h_Watchdog is now equal to \t" + getLocal_1h_watchdog());
		    					}
		    					
		    					// conversion from [s] (getLocal_1h_watchdog) to [ms] (setDelay) is required, hence multiplication by 1000
		    					setDelay((int)1000 * getLocal_1h_watchdog()/2);
		    					// conversion from [ms] (getDelay) to [s] (getLocal_1h_watchdog) is required, hence multiplication by 0.001
			    				setLocal_1h_watchdog(getLocal_1h_watchdog() - ((double)((0.001 * getDelay()))));
			    				
			    				setDelay(1000 * getLocal_watchdog_scale_factor());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog global: \t" +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog global: \t" +  Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1h_Watchdog updated including the delay for ServerMessage_Request_MeasurementData: \t" + getLocal_1h_watchdog());
		    				}
		    				else if(getLocal_24h_watchdog() < (300 * getLocal_watchdog_scale_factor()) ) {
		    					
		    					if(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration()) {
		    						//time_offset = Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() ; 
		    						System.out.println("[Compute engine Runnable] " +sensor.getSensorID()+" higher Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(): " + Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						System.out.println("[Compute engine Runnable] " +sensor.getSensorID()+" Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(): " + Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						
		    					}
		    					else {
		    						//time_offset = Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() ;
		    						System.out.println("[Compute engine Runnable] " +sensor.getSensorID()+" lesser Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration(): " + Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						System.out.println("[Compute engine Runnable] " +sensor.getSensorID()+" Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration(): " + Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    					}
		    					if(getLocal_24h_watchdog() > (150 * getLocal_watchdog_scale_factor()) ) {
		    						// this delay is intended to synchronize the measurements from all sensor if their Local_24h_watchdogs have different values that are far from the threshold
		    						processingDelay((int)1000 * getLocal_24h_watchdog()*0.5);
		    						setLocal_24h_watchdog(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
		    						//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Synchronizig 24h_Watchdogs by activating an additional delay, 24h_Watchdog is now equal to \t" + getLocal_24h_watchdog());
		    					}
		    					
			    				// conversion from [s] (getLocal_1h_watchdog) to [ms] (setDelay) is required, hence multiplication by 1000
			    				setDelay((int)1000 * getLocal_24h_watchdog()/2);
			    				// conversion from [ms] (getDelay) to [s] (getLocal_1h_watchdog) is required, hence multiplication by 0.001
			    				setLocal_24h_watchdog(getLocal_24h_watchdog() - ((double)((0.001 * getDelay()))));
			    				
			    				// delay is set to a minimal value to prevent 1h Watchdog from expiration
			    				setDelay(1000 * getLocal_watchdog_scale_factor());
			    				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24h_Watchdog updated including the delay for ServerMessage_Request_MeasurementHistory: \t" + getLocal_24h_watchdog());
			    				request_measurement_history = true;
		    				}
		    				else {
		    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_ACK received when neither 1h_watchdog: " + getLocal_1h_watchdog() + " [s] nor 24h_watchdog: " + getLocal_24h_watchdog() + " [s] is close to expire");
		    					
		    					// indicate that ComputeEngine_Runnable should be closed
		    					// ClientMessage_ACK has been sent for the confirmation purposes, hence there are no further messages expected
		    					close_ComputeEngine_Runnable = true;
		    				}
		    			}
		    			else if (receivedMessage instanceof ClientMessage_SensorInfo) {
		    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_SensorInfo message from sensor: " + sensor.getSensorID() + " has been received.");
		    				////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] ClientMessage_SensorInfo message has the following timestamp: " + receivedMessage.getTimestamp());
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
		        				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] send go to OPERATIONAL to sensor: " + sensor.getSensorID());
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
		    					
		    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] send new setting (go to MAINTENANCE) to sensor: " + sensor.getSensorID());
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
	    					setLocal_1h_watchdog(Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() + Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
        					/*  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() is used (as the offset argument)
        					* for extending TimeLeftBeforeExpiration in the next measurement data iteration by adding the remaining time to expiration from the current 1h Watchdog iteration
	    					* setLocal_1h_watchdog( Global_1h_Watchdog.getInstance().getExpiration() * getLocal_watchdog_scale_factor() +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
	    					*/
	    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Local_1h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_1h_watchdog());
	        				
	    					// send ServerMessage_ACK
	        				sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID(),
									getLocal_1h_watchdog(), getLocal_24h_watchdog()));
	        				
	        				//indicate to the TCPserver that the 1h watchdog for this sensor has been kicked
	        				set_1hWatchog_Timestamp_tableID_value(true, receivedMessage.getSensorID() - 1);
	        				
	        				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementData from sensor: " + sensor.getSensorID() + " has been received.");
	        				////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementData message has the following timestamp: " + receivedMessage.getTimestamp());
	        	
	    				}
	    				else if (receivedMessage instanceof ClientMessage_MeasurementHistory) {
	    					
	    					MeasurementData[] mes_hist = ((ClientMessage_MeasurementHistory) receivedMessage).getMes_history();

	    					
	    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementHistory from sensor: " + sensor.getSensorID() + " has been received.");
	    					////System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] MeasurementHistory message has the following timestamp: " + receivedMessage.getTimestamp());
	    					

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
	    					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Local_24h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_24h_watchdog());
	    					
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
    			
					if (getLocal_1h_watchdog() <= 100 * getLocal_watchdog_scale_factor()) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementData(sensor.getSensorID()));
	    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Request_MeasurementData is being send to sensor ID: " + sensor.getSensorID());
		    		}
		    		else if ((getLocal_24h_watchdog() <= 100 * getLocal_watchdog_scale_factor()) && request_measurement_history) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementHistory(sensor.getSensorID()));
	    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Request_MeasurementHistory is being send to sensor ID: " + sensor.getSensorID());
	    				request_measurement_history = false;
		    		}
		    		else if (close_ComputeEngine_Runnable) {
		    			setComputeEngine_Runnable_running(false);
		    		}
				}
    			else {
    				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] for sensor ID: " + sensor.getSensorID() + " is being closed");
    				break;
    			}
    		}
		} catch (IOException IOex) {
        	//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Error: when attempted to read Object from inputStream or write Object to outputStream on the server side");
        	IOex.printStackTrace();
        } catch (ClassNotFoundException CNFex) {
        		//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Error: when attempted to servialize/deserialize objects on the server side");
        		CNFex.printStackTrace();
		} finally {
			// check all local 1h watchdogs - if all have been updated - kick the 1h watchdog
			if(areAllTrue(get_1hWatchog_timestamp_table().get()) ) {
				
				// 1h Wathdog is kicked here for all measurement data iterations with an exception for the measurement history iteration
				// it is to make sure that 1h Wathdog will be kicked once the last instance of sensor sends its measurement history
				if(!isIDTrue(get_24hWatchog_timestamp_table().get(), sensor.getSensorID()) && (Global_1h_Watchdog.getInstance().getTimeFromLastFeed() > time_offset)) {
					
					
					// reduce the 24h Watchdog time before expiration by the remaining time from 1h Watchdog that left before 1h Watchdog has been kicked - it has to be done to avoid desynchronization
					//Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration( Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() +  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
					// add an offset to 24h_Watchdog TimeLeftBeforeExpiration (the offset is value of 1hWatchdog when it has been kicked)
					//Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration()
										
					time_offset =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
					computing_time = computing_time - time_offset;
					// kick 1h Global Watchdog
					Global_1h_Watchdog.getInstance().feed();
					Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + time_offset);
					Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + computing_time);
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked");
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked when it has: "+time_offset+ " [s] left to expire");
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] it took: "+computing_time+ " [s] to execute TCP connection");
					//  once 1h Watchdog is kicked - set local 1h watchdog flags in the 24hWatchog_timestamp_table array to FALSE
					set_1hWatchog_Allfalse();
				}
				// check all local 24h watchdogs - if they haven't been updated - do not kick 1h watchdog
				else if (!areAllTrue(get_24hWatchog_timestamp_table().get())) {
					//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has not been kicked");
				}
				// check all local 24h watchdogs - if they have been updated - 1h watchdog will be kicked in the next if statement
				else {
					time_offset =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
					computing_time = computing_time - time_offset;
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has not been kicked");
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] it took: "+computing_time+ " [s] to execute TCP connection");
				}
			}
			// check all local 24h watchdogs - if all have been updated - kick the 24h watchdog & 1h watchdog
			if(areAllTrue(get_24hWatchog_timestamp_table().get()) && (Global_24h_Watchdog.getInstance().getTimeFromLastFeed() > time_offset)) {
				// reduce the 24h Watchdog time before expiration by the remaining time from 1h Watchdog that left before 1h Watchdog has been kicked - it has to be done to avoid desynchronization
				 //Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration( Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() -  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
				
				if (Global_1h_Watchdog.getInstance().getTimeFromLastFeed() > time_offset){
					time_offset =  Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration();
					// kick 1h Global Watchdog
					Global_1h_Watchdog.getInstance().feed();
					Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + time_offset);
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked");
					System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 1hWatchdog has been kicked when it has: "+time_offset+ " [s] left to expire");
					
					// once 1h Watchdog is kicked - set local 1h watchdog flags in the 24hWatchog_timestamp_table array to FALSE
					set_1hWatchog_Allfalse();
				}
				// kick 24h Global Watchdog
				 Global_24h_Watchdog.getInstance().feed();
				 Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + time_offset);
				System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24hWatchdog has been kicked");				
				
			}
			else {
				//System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] 24hWatchdog has not been kicked");
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
				processingDelay(1000);
				set_24hWatchog_Allfalse();
			}
        	try {
        		//processingDelay(100);
        		closeOutStream();
				closeInStream();
			} catch (IOException IOex) {
			    //System.out.println("[Compute engine Runnable " +sensor.getSensorID()+"] Error: when attempted to close InputStreamReader inputStream on the server side");
			    IOex.printStackTrace();
			}
        }
    }
	
	public void closeOutStream() throws IOException {
		if (outputStream!=null) {
			outputStream.close();
		}
	}
	
	public void closeInStream() throws IOException {
		if (inputStream!=null) {
			inputStream.close();
		}
	}
	
	public ObjectOutputStream getOutputStream() {
		return this.outputStream;
	}

	public ObjectInputStream getInputReaderStream() {
		return this.inputStream;
	}
	
	static void processingDelay(double msec) {
	    try {
	        Thread.sleep( (int) msec);
	    } catch (InterruptedException ex) {
	        
	    }
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


}

