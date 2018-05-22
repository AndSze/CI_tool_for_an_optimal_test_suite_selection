package tcpServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
	
	public ComputeEngine_Runnable(Socket clientSocket) throws IOException  {
		super();
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
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
	    		if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null) {
	    			
    				sensor = getProcessing_engine().searchInServerSensorList(receivedMessage.getSensorID());
					
	    			if (receivedMessage instanceof ClientMessage_BootUp) {
	    										
	    				System.out.println("[Compute engine Runnable] BootUp message from sensor: " + sensor.getSensorID() + " has been received.");
	    				System.out.println("[Compute engine Runnable] BootUp message has the following timestamp: " + receivedMessage.getTimestamp());
	    				// send ServerMessage_SensorInfoQuerry
	    				sendMessage(new ServerMessage_SensorInfoQuerry(receivedMessage.getSensorID()));
	    			}
	    			else if (receivedMessage instanceof ClientMessage_SensorInfo) {
	    				System.out.println("[Compute engine Runnable] ClientMessage_SensorInfo message from sensor: " + sensor.getSensorID() + " has been received.");
	    				System.out.println("[Compute engine Runnable] ClientMessage_SensorInfo message has the following timestamp: " + receivedMessage.getTimestamp());
	    				SensorImpl received_sensor = ((ClientMessage_SensorInfo) receivedMessage).getSensor();
	    				
	    				System.out.println("sensor.getCoordinates(): "+ sensor.getCoordinates() + "\t received_sensor.getCoordinates(): "+ received_sensor.getCoordinates());
	    				System.out.println("sensor.getSoftwareImageID(): "+ sensor.getSoftwareImageID()+ "\t received_sensor.getSoftwareImageID(): "+ received_sensor.getSoftwareImageID());
	    				System.out.println("sensor.getSensorState(): "+ sensor.getSensorState()+ "\t received_sensor.getSensorState(): "+ received_sensor.getSensorState());
	    				
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
	    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState()));
	    					
	    					// set to 1hWatchdog 120 [s] to activate the client socket (out/in object streams)
	        				get_1hWatchdog_INSTANCE().setTimeLeftBeforeExpiration(120);
	        				
	        				System.out.println("[Compute engine Runnable] send go to OPERATIONAL");
	    					
	    				}
	    				else {
	    					// received sensor info is out of date
	    					// send new settings and force the sensor to reset
	    					
	    					sensor.setSensorState(SensorState.MAINTENANCE);
	    					Server_Sensors_LIST = getProcessing_engine().updateServerSensorList(sensor);
	    					
	    					// serialize sensor instance and save it to file
	    					getProcessing_engine().saveSensorInfo(sensor);
	    					
	    					// send ServerMessage_SensorInfoUpdate that changes triggers the sensor to reset, then the sensor should send ClientMessage_BootUp
	    					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState()));
	    					
	    					System.out.println("[Compute engine Runnable] send new setting (go to MAINTENANCE)");
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
    					
    					// send ServerMessage_ACK
        				sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID()));
    					
        				System.out.println("[Compute engine Runnable] received MeasurementData");
        				System.out.println("[Compute engine Runnable] MeasurementData message has the following timestamp: " + receivedMessage.getTimestamp());
        				// feed 1hWatchdog
        				get_1hWatchdog_INSTANCE().feed();
    				}
    				else if (receivedMessage instanceof ClientMessage_MeasurementHistory) {
    					
    					MeasurementData[] mes_hist = ((ClientMessage_MeasurementHistory) receivedMessage).getMes_history();
    					
    					System.out.println("[Compute engine Runnable] received MeasurementHistory");
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
        					sendMessage(new ServerMessage_SensorInfoUpdate(sensor.getSensorID(), sensor.getCoordinates(), sensor.getSoftwareImageID(), sensor.getSensorState()));
        					
        					System.out.println("[Compute engine Runnable] MeasurementHistory does not match the MeasurementData");
        					System.out.println("[Compute engine Runnable] Sensor instance: " + sensor.getSensorID() + " is dead");
    					}
    					
    					// send ServerMessage_ACK
        				sendMessage(new ServerMessage_ACK(receivedMessage.getSensorID()));
    					
        				// feed 1hWatchdog
        				get_24hWatchdog_INSTANCE().feed();
    				}
		    		
	    		}
	    		if (get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration() < 120) {
	    			if (sensor.getSensorID() != 0) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementData(sensor.getSensorID()));
	    			}
	    			else {
	    				throw new IllegalArgumentException("sensor_ID is not specified for ComputeEngine_Runnable (ServerMessage_Request_MeasurementData)");
	    			}
	    		}
	    		else if (get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration() < 120) {
	    			if (sensor.getSensorID() != 0) {
	    				// send ServerMessage_Request_MeasurementData
	    				sendMessage(new ServerMessage_Request_MeasurementHistory(sensor.getSensorID()));
	    			}
	    			else {
	    				throw new IllegalArgumentException("sensor_ID is not specified for ComputeEngine_Runnable (ServerMessage_Request_MeasurementHistory)");
	    			}
	    		}
	    		// else statement forces the loop to stop - however it should be never executed since the ComputeEngine_Runnable starts upon receiving ClientMessage_BootUp
	    		else {
						processingDelay(1000);
						System.out.println("1h_Watchdog: " + get_1hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
						System.out.println("24h_Watchdog: " + get_24hWatchdog_INSTANCE().getTimeLeftBeforeExpiration());
		   		} 
    		}
		} catch (IOException IOex) {
        	System.out.println("Error: when attempted to read Object from inputStream or write Object to outputStream on the server side");
        	IOex.printStackTrace();
        } catch (ClassNotFoundException CNFex) {
        		System.out.println("Error: when attempted to servialize/deserialize objects on the server side");
        		CNFex.printStackTrace();
		} finally {
        	try {
        		closeOutStream();
				closeInStream();
			} catch (IOException IOex) {
			    System.out.println("Error: when attempted to close InputStreamReader inputStream on the server side");
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
}

