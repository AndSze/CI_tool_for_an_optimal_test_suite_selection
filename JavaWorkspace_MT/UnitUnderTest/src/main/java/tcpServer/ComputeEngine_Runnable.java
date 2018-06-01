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

public class ComputeEngine_Runnable extends TCPserver implements Runnable {
	
    private ObjectOutputStream outputStream = null;
    private ObjectInputStream inputStream = null;
    private SensorImpl sensor = null;
    private boolean isComputeEngine_Runnable_running = false;
    private int delay = 100;
    double local_1h_watchdog = 0;
    double local_24h_watchdog = 0;

	public ComputeEngine_Runnable(Socket clientSocket, boolean isComputeEngine_Runnable_running) throws IOException  {
		super();
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        this.isComputeEngine_Runnable_running = isComputeEngine_Runnable_running;
        this.local_1h_watchdog = get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration();
        this.local_24h_watchdog = get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration();
        System.out.println("[Compute engine Runnable] Multithreaded Server Service has been started");
	}

	public void sendMessage(Message_Interface message) throws IOException {
		
        // it sends the message via output stream
			getOutputStream().writeObject(message);
        
	}

    public void run() {
        
    	//synchronized (Echo) {
    	Message_Interface receivedMessage = null;
    	try {
    		while(true) {
    			
    			setDelay(100);
    			if(isComputeEngine_Runnable_running()) {
    				
					if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null) {
		    			
	    				sensor = getProcessing_engine().searchInServerSensorList(receivedMessage.getSensorID());
						
		    			if (receivedMessage instanceof ClientMessage_BootUp) {
		    										
		    				System.out.println("[Compute engine Runnable] BootUp message from sensor: " + sensor.getSensorID() + " has been received.");
		    				System.out.println("[Compute engine Runnable] BootUp message has the following timestamp: " + receivedMessage.getTimestamp());
		    				// send ServerMessage_SensorInfoQuerry
		    				sendMessage(new ServerMessage_SensorInfoQuerry(receivedMessage.getSensorID()));
		    			}
		    			else if (receivedMessage instanceof ClientMessage_ACK) {
		    				setDelay(2000);
		    				System.out.println("[Compute engine Runnable] received ClientMessage_ACK from sensor: " + sensor.getSensorID() + " has been received.");
		    				//System.out.println("[Compute engine Runnable] Processing Delay set to: " + getDelay());
		    				System.out.println("[Compute engine Runnable] 1h_Watchdog global: \t" + get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
		    				System.out.println("[Compute engine Runnable] 1h_Watchdog local: \t" + getLocal_1h_watchdog());
		    				if(getLocal_1h_watchdog()<12) {
			    				setLocal_1h_watchdog(getLocal_1h_watchdog() - (getDelay()/1000));
			    				System.out.println("[Compute engine Runnable] 1h_Watchdog updated including the delay for ServerMessage_Request_MeasurementData: \t" + getLocal_1h_watchdog());
		    				}
		    				if(getLocal_24h_watchdog()<22) {
			    				setLocal_24h_watchdog(getLocal_24h_watchdog() - (getDelay()/1000));
			    				System.out.println("[Compute engine Runnable] 24h_Watchdog updated including the delay for ServerMessage_Request_MeasurementHistory: \t" + getLocal_1h_watchdog());
		    				}
		    			}
		    			else if (receivedMessage instanceof ClientMessage_SensorInfo) {
		    				System.out.println("[Compute engine Runnable] ClientMessage_SensorInfo message from sensor: " + sensor.getSensorID() + " has been received.");
		    				System.out.println("[Compute engine Runnable] ClientMessage_SensorInfo message has the following timestamp: " + receivedMessage.getTimestamp());
		    				SensorImpl received_sensor = ((ClientMessage_SensorInfo) receivedMessage).getSensor();
		    				
		    				/*
		    				System.out.println("sensor.getCoordinates(): "+ sensor.getCoordinates() + "\t received_sensor.getCoordinates(): "+ received_sensor.getCoordinates());
		    				System.out.println("sensor.getSoftwareImageID(): "+ sensor.getSoftwareImageID()+ "\t received_sensor.getSoftwareImageID(): "+ received_sensor.getSoftwareImageID());
		    				System.out.println("sensor.getSensorState(): "+ sensor.getSensorState()+ "\t received_sensor.getSensorState(): "+ received_sensor.getSensorState());
		    				*/
		    				
		    				if ((sensor.getCoordinates().equals(received_sensor.getCoordinates())) &&
		    					(sensor.getSoftwareImageID().equals(received_sensor.getSoftwareImageID())) &&
		    					(sensor.getSensorState().equals(received_sensor.getSensorState()))){
		    					
		    					// received sensor info is up to date
		    					// send go to operational by updating the sensor state
		    					
		    					sensor.setSensorState(SensorState.OPERATIONAL);
		    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
		    					
		    					// serialize sensor instance and save it to file
		    					getProcessing_engine().saveSensorInfo(sensor);
		    					
		    					// send ServerMessage_SensorInfoUpdate that changes only state of the sensor to SensorState.OPERATIONAL
		    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    								get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration(), get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration()));
		    					
		        				System.out.println("[Compute engine Runnable] send go to OPERATIONAL to sensor: " + sensor.getSensorID());
		        				//setComputeEngine_Runnable_running(false);
		        				
		    				}
		    				else {
		    					// received sensor info is out of date
		    					// send new settings and force the sensor to reset
		    					
		    					sensor.setSensorState(SensorState.MAINTENANCE);
		    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
		    					
		    					// serialize sensor instance and save it to file
		    					getProcessing_engine().saveSensorInfo(sensor);
		    					
		    					// set to 1hWatchdog 120 [s] to activate the client socket (out/in object streams)
		        				// get_1hWatchdog_INSTANCE().setTimeLeftBeforeExpiration(30);
		    					
		    					// send ServerMessage_SensorInfoUpdate that changes triggers the sensor to reset, then the sensor should send ClientMessage_BootUp
		    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),
		    								get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration(), get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration()));
		    					
		    					System.out.println("[Compute engine Runnable] send new setting (go to MAINTENANCE) to sensor: " + sensor.getSensorID());
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
	    					
	    					// feed local watchdog for the sensor
	    					setLocal_1h_watchdog(get_1hWatchdog_INSTANCE().getExpiration());
	        				//get_1hWatchdog_INSTANCE().feed(get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
	    					System.out.println("[Compute engine Runnable] Local_1h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_1h_watchdog());
	        				
	    					// send ServerMessage_ACK
	        				sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID(),
									getLocal_1h_watchdog(), getLocal_24h_watchdog()));
	        				
	        				//indicate to the TCPservet that the 1h watchdog for this sensor has been kicked
	        				set_1hWatchog_Timestamp_tableID_value(true, receivedMessage.getSensorID() - 1);
	        				
	        				System.out.println("[Compute engine Runnable] MeasurementData from sensor: " + sensor.getSensorID() + " has been received.");
	        				System.out.println("[Compute engine Runnable] MeasurementData message has the following timestamp: " + receivedMessage.getTimestamp());
	        	
	    				}
	    				else if (receivedMessage instanceof ClientMessage_MeasurementHistory) {
	    					
	    					MeasurementData[] mes_hist = ((ClientMessage_MeasurementHistory) receivedMessage).getMes_history();
	    					
	    					System.out.println("[Compute engine Runnable] MeasurementHistory from sensor: " + sensor.getSensorID() + " has been received.");
	    					System.out.println("[Compute engine Runnable] MeasurementHistory message has the following timestamp: " + receivedMessage.getTimestamp());
	    					
	    					if(getProcessing_engine(). compareMeasurementDataAgainstMeasurementHistory(sensor, mes_hist)) {
	    						// serialize measurement data instance and save it to file
	        					getProcessing_engine().saveMeasurementHistoryInfo(sensor, mes_hist);
	        					getProcessing_engine().deleteMeasurementDataInfo(sensor);
	        					
	        					System.out.println("[Compute engine Runnable] MeasurementHistory matches the MeasurementData");
	        					sensor.resetSensor();
	        					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
	    					}
	    					else {
	    						sensor.setSensorState(SensorState.DEAD);
	        					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
	        					
	        					// send ServerMessage_SensorInfoUpdate that indicates that something is wrong with the sensor
	        					// write 0 [s] to 1hWatchdog and 24hWatchdog to indicate that this sensor is dead
	        					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState(),0 ,0));
	        					setComputeEngine_Runnable_running(false);
		        				
	        					System.out.println("[Compute engine Runnable] MeasurementHistory does not match the MeasurementData");
	        					System.out.println("[Compute engine Runnable] Sensor instance: " + sensor.getSensorID() + " is dead");
	    					}
	    					
	        				// feed 24hWatchdog
	        				//get_24hWatchdog_INSTANCE().feed(get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
	    					setLocal_24h_watchdog(get_24hWatchdog_INSTANCE().getExpiration());
	    					System.out.println("[Compute engine Runnable] Local_24h_watchdog has been kicked for sensor: " + sensor.getSensorID() + " and it is equal to: " + getLocal_24h_watchdog());
	    					
	    					//indicate to the TCPservet that the 24h watchdog for this sensor has been kicked
	    					//set_2hWatchog_Timestamp_tableID_value(true, receivedMessage.getSensorID() - 1);
	        				
	    					// send ServerMessage_ACK
	    					sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID(),
									getLocal_1h_watchdog(), getLocal_24h_watchdog()));
	    				}
		    		}
					processingDelay(getDelay());
					System.out.println("[Compute engine Runnable] Processing Delay is set to: " + getDelay());
    				System.out.println("[Compute engine Runnable] 1h_Watchdog local: \t" + getLocal_1h_watchdog());
    				System.out.println("[Compute engine Runnable] 24h_Watchdog local: \t" + getLocal_24h_watchdog());
					if (getLocal_1h_watchdog() <= 10) {
		    			if (sensor.getSensorID() != 0) {
		    				// send ServerMessage_Request_MeasurementData
		    				sendMessage(new ServerMessage_Request_MeasurementData(sensor.getSensorID()));
		    				System.out.println("[Compute engine Runnable] Request_MeasurementData is being send to sensor ID: " + sensor.getSensorID());
		    				
		    			}
		    			else {
		    				throw new IllegalArgumentException("[Compute engine Runnable] sensor_ID is not specified for ComputeEngine_Runnable (ServerMessage_Request_MeasurementData)");
		    			}
		    		}
		    		else if (getLocal_24h_watchdog() <= 20) {
		    			if (sensor.getSensorID() != 0) {
		    				// send ServerMessage_Request_MeasurementData
		    				sendMessage(new ServerMessage_Request_MeasurementHistory(sensor.getSensorID()));
		    			}
		    			else {
		    				throw new IllegalArgumentException("[Compute engine Runnable] sensor_ID is not specified for ComputeEngine_Runnable (ServerMessage_Request_MeasurementHistory)");
		    			}
		    		}
		    		else if (getDelay() == 2000 ) {
		    			setComputeEngine_Runnable_running(false);
		    		}
				}
    			else {
    				System.out.println("[Compute engine Runnable] for sensor ID: " + sensor.getSensorID() + " is being closed");
    				break;
    			}
    		}
		} catch (IOException IOex) {
        	System.out.println("[Compute engine Runnable] Error: when attempted to read Object from inputStream or write Object to outputStream on the server side");
        	IOex.printStackTrace();
        } catch (ClassNotFoundException CNFex) {
        		System.out.println("[Compute engine Runnable] Error: when attempted to servialize/deserialize objects on the server side");
        		CNFex.printStackTrace();
		} finally {
			// check all local watchdogs - if all have been updated - kick the 1h watchdog
			if(areAllTrue(get_1hWatchog_timestamp_table().get())) {
				if(get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration()<10) {
					get_1hWatchdog_INSTANCE().feed(get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
					System.out.println("[TCPserver] 1hWatchdog has been kicked");
					set_1hWatchog_Allfalse();
				}
			}
			//check all local watchdogs - if all have been updated - kick the watchdog
			/*if(areAllTrue(get_24hWatchog_timestamp_table())) {
				get_1hWatchdog_INSTANCE().feed(get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
				System.out.println("[TCPserver] 24hWatchdog has been kicked");
				setAllfalse(get_24hWatchog_timestamp_table());
			}*/
        	try {
        		closeOutStream();
				closeInStream();
			} catch (IOException IOex) {
			    System.out.println("[Compute engine Runnable] Error: when attempted to close InputStreamReader inputStream on the server side");
			    IOex.printStackTrace();
			}
        }
    }
    
    
    /*	
    		try {
    		BufferedReader bufferedReader = new BufferedReader(inputStream);
            String message = null;
            timeout = 0;
            
            while(timeout<10)
            {
    			if(bufferedReader.ready())
    			{
	            	long time = System.currentTimeMillis();
	            	message = bufferedReader.readLine();
	            	EchoResponse(outputStream, message, time);
	            	timeout = 0;
    			}
    			else
    			{
    				timeout = timeout+1;
    				processingDelay(1000);
    			}
    			//processingDelay(10);
            }  
        } catch (IOException IOex) {
        	System.out.println("Error: when attempted to read bufferedReaderinputStream on the client side");
        	IOex.printStackTrace();
        } finally {
        		closeOutStream();
        	try {
				closeInStream();
			} catch (IOException IOex) {
			    System.out.println("Error: when attempted to close InputStreamReader inputStream on the client side");
			    IOex.printStackTrace();
			}
          }
    	//}
    }
    */
    /*
	public  void EchoResponse(PrintStream outputStream, String message, long time) {

		String server_message = null;
		
        System.out.println("message received from cliennnt: \n\t"+message);
        //processingDelay(1000);
        server_message =  Integer.toString(computeEnginesRunningID*Integer.parseInt(message));
        server_message = "Let's try ComputerEngine ID: " + computeEnginesRunningID+ " that resends: "+server_message;
        
        System.out.println("Send back the following message: "+server_message);
        
        outputStream.println(server_message);
        System.out.println("Request processed: " + time);
	}*/
	
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
	
	static void processingDelay(int msec) {
	    try {
	        Thread.sleep(msec);
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

	public void setDelay(int delay) {
		this.delay = delay;
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

}

