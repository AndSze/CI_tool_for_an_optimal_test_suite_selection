package tcpClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
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
import watchdog.Local_1h_Watchdog;

public class ClientManager implements TCPclient_interface{

	private ObjectOutputStream outputStream = null;
	private ObjectInputStream inputStream = null;
	private boolean isClientManagerRunning = false;
	private int sensor_ID = 0;
	SensorImpl sensor = null;
	
	// default constructor 
	public ClientManager() {
		// to avoid calling multiple times the constructor of the TCPclient class 
		super();
	}
	
	// overloaded constructor
	private ClientManager(ObjectOutputStream outputStream, ObjectInputStream inputStream, int sensor_ID){
		this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.sensor_ID = sensor_ID;
        isClientManagerRunning = true;
	}

	public ClientManager initClientManager(Socket clientSocket, int sensor_ID) throws IOException{
		// it activates serverSocket.accept() on the server side
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        return (new ClientManager(outputStream, inputStream, sensor_ID));
	       
	}

	public void sendMessage(Message_Interface message) throws IOException {
		
		// sends message from the client output stream to the server input stream
		getOutputStream().writeObject(message); 
        
	}
	
	public void messagesHandler(ObjectOutputStream outputStream, ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
		
		Message_Interface receivedMessage = null;
		boolean ack_alert = false;
		boolean wait_for_measurement_data = false;
		while(true)
        {
			if (isClientManagerRunning()) {
				if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null) {
					sensor = TCPclient.searchInClientSensorList(sensor_ID);
					if (receivedMessage instanceof ServerMessage_Request_MeasurementData) {
						if (sensor != null && wait_for_measurement_data) {
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_Request_MeasurementData has been received.");
							//System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_Request_MeasurementData has the following timestamp: " + receivedMessage.getTimestamp());
							double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
							double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
							int humidity = ThreadLocalRandom.current().nextInt(0, 101);
							int temperature = ThreadLocalRandom.current().nextInt(0, 30);
							int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
							sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
							
							MeasurementData mes_data = sensor.readLastMeasurementData();
							TCPclient.updateClientSensorList(sensor);
							/*
							SensorImpl temp_sens = sensor = TCPclient.searchInClientSensorList(sensor_ID);
							mes_data = temp_sens.readLastMeasurementData();
							System.out.println("\t\t[ClientManager" +sensor.getSensorID()+"] readLastMeasurementData()\t Pm25: "+ mes_data.getPm25() + "\t Pm10: "+ mes_data.getPm10()  + "\t humidity: "+ mes_data.getHumidity());
							*/
							System.out.println("[ClientManager " +sensor.getSensorID()+"] sensor: \t" + sensor.getSensorID() + " has the following number of measurements: \t"  + sensor.getNumberOfMeasurements());
							
							if (sensor.getNumberOfMeasurements() == TCPclient.getMeasurements_limit()) {
								System.out.println("[ClientManager " +sensor.getSensorID()+"] ack_alert is set to true - the sensor waits for ServerMessage_Request_MeasurementHistory.");
								ack_alert = true;
							}
							
							sendMessage(new ClientMessage_MeasurementData(sensor_ID, mes_data));
							TCPclient.updateClientSensorList(sensor);
							
							wait_for_measurement_data = false;
						}
					}
					else if (receivedMessage instanceof ServerMessage_Request_MeasurementHistory) {
						if (sensor != null) {
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_Request_MeasurementHistory has been received.");
							System.out.println("[ClientManager " +sensor.getSensorID()+"] sensor: \t" + sensor.getSensorID() + " has the following number of measurements: \t"  + sensor.getNumberOfMeasurements());
							//System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_Request_MeasurementHistory has the following timestamp: " + receivedMessage.getTimestamp());
							MeasurementData[] mes_data = sensor.readMeasurementHistory();
							sendMessage(new ClientMessage_MeasurementHistory(sensor_ID, mes_data));
							
							// to clear the measurement history and set number of measurement to 0
							System.out.println("[ClientManager " +sensor.getSensorID()+"] Sensor is being reset once ClientMessage_MeasurementHistory is sent");
							sensor.resetSensor();
							TCPclient.updateClientSensorList(sensor);
							
							ack_alert = false;
							
						}
					}
					else if (receivedMessage instanceof ServerMessage_SensorInfoQuerry) {
						if (sensor != null) {
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_SensorInfoQuerry has been received.");
							sendMessage(new ClientMessage_SensorInfo(sensor));
						}
					}
					else if (receivedMessage instanceof ServerMessage_SensorInfoUpdate) {
						if (sensor != null) {
							SensorImpl new_sensor = new SensorImpl(receivedMessage.getSensorID(), 
																((ServerMessage_SensorInfoUpdate) receivedMessage).getCoordinates(), 
																((ServerMessage_SensorInfoUpdate) receivedMessage).getSoftwareImageID(),
																((ServerMessage_SensorInfoUpdate) receivedMessage).getMeasurements_limit());
							
							new_sensor.setSensorState(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensorState());
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_SensorInfoUpdate has been received.");
							//System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_SensorInfoUpdate has the following timestamp: " + receivedMessage.getTimestamp());
							new_sensor.setSensor_watchdog_scale_factor(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensor_watchdog_scale_factor());
							
							if (new_sensor.getSensorState() == SensorState.MAINTENANCE || new_sensor.getSensorState() == SensorState.PRE_OPERATIONAL) {
						
								SensorState previousSensorState = new_sensor.getSensorState();
								new_sensor.resetSensor();
								
								// send BootUp message
						    	sendMessage(new ClientMessage_BootUp(sensor_ID));
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] Boot Up message send by the Client after processing ServerMessage_SensorInfoUpdate");
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] Sensor is being reset once ClientMessage_BootUp is being sent");
						    	// reset Sensor results in setting its state to PRE_OPERATIONAL, hence the state has to be updated to MAINTENANCE
						    	if (previousSensorState == SensorState.MAINTENANCE) {
						    		new_sensor.setSensorState(SensorState.MAINTENANCE);
						    	}
								TCPclient.updateClientSensorList(new_sensor);
								
							}
							else if (new_sensor.getSensorState() == SensorState.OPERATIONAL) {
								
								// send ACK message to indicate that the configuration is successful 
						    	sendMessage(new ClientMessage_ACK(sensor_ID));
						    	
						    	sensor.setSensorState(SensorState.OPERATIONAL);
						    	sensor.setSensor_watchdog_scale_factor(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensor_watchdog_scale_factor());
			
						    	TCPclient.updateClientSensorList(sensor);
								
						    	wait_for_measurement_data = true;
							}
							else if (new_sensor.getSensorState() == SensorState.DEAD) {
								sendMessage(new ClientMessage_ACK(sensor_ID));
								setClientManagerRunning(false);
								System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_SensorInfoUpdate has the following SensorState: " + new_sensor.getSensorState());
								System.out.println("[ClientManager " +sensor.getSensorID()+"] SensorState.DEAD causes the connection with the server to stop "); 
								
								TCPclient.updateClientSensorList(new_sensor);
							}
							
							// set the input parameters for the local watchdogs on the client side
							TCPclient.setWatchdogs_scale_factor(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensor_watchdog_scale_factor());
							TCPclient.setMeasurements_limit(((ServerMessage_SensorInfoUpdate) receivedMessage).getMeasurements_limit());
							
							// activate the local watchdogs on the client side
							Local_1h_Watchdog.getInstance().setEnabled(isClientManagerRunning());
							Local_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_SensorInfoUpdate) receivedMessage).get1h_Watchdog());
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] Local_1h_Watchdog for the sensor has been synchronized and it equals: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]" );
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_SensorInfoUpdate has the following SensorState: " + new_sensor.getSensorState());

							
							if(new_sensor.getSensorState() == SensorState.OPERATIONAL && Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 1200 * ((ServerMessage_SensorInfoUpdate) receivedMessage).getSensor_watchdog_scale_factor()) {
								System.out.println("[ClientManager " +sensor.getSensorID()+"] if sensor receives go to OPERATIONAL, ClientManager is being closed"); 
								System.out.println("[ClientManager " +sensor.getSensorID()+"] it will be launched agan once - _1h_Watchdog: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + "[s] is close to expire.");
								setClientManagerRunning(false);
							}

						}
					}
					else if (receivedMessage instanceof ServerMessage_ACK) {
						
						System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_ACK has been received.");
						//System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_ACK has the following timestamp: " + receivedMessage.getTimestamp());

						
						// ack_alert equals TRUE only if (sensor.getNumberOfMeasurements() == 24), hence setClientManagerRunning(false) has to be executed in the subsequent loop
						// upon receiving ClientMessage_MeasurementHistory
						
						if(!ack_alert) {
							setClientManagerRunning(false);
							// updating the 1h Watchdog time before expiration is required here when the sensor is in a different from the OPERATIONAL state
							Local_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_ACK) receivedMessage).get1h_Watchdog());
							System.out.println("[ClientManager " +sensor.getSensorID()+"] Local_1h_Watchdog for the sensor has been synchronized and it equals: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]" );
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ClientMessage_ACK has been sent to end the server-client connection successfully");
							// send ACK message to disable the socket on the server side
					    	sendMessage(new ClientMessage_ACK(sensor_ID));
					    	Local_1h_Watchdog.getInstance().setEnabled(true);
						}
						else {
							// send ACK message to confirm that ClientMessage_MeasurementData has been sent, but do not disable the socket on the server side - the sensor waits for ServerMessage_Request_MeasurementHistory
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ClientMessage_ACK has been sent to confirm that ClientMessage_MeasurementData has been sent - wait for ServerMessage_Request_MeasurementHistory");
							sendMessage(new ClientMessage_ACK(sensor_ID));
							Local_1h_Watchdog.getInstance().setEnabled(false);
						}

					}
				}
			} 
			else {
				System.out.println("[ClientManager " +sensor.getSensorID()+"] is being closed for sensor ID: " + sensor_ID);
				break;
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
	
	public synchronized boolean isClientManagerRunning() {
		return isClientManagerRunning;
	}

	public synchronized void setClientManagerRunning(boolean isClientManagerRunning) {
		this.isClientManagerRunning = isClientManagerRunning;
	}
	
}
