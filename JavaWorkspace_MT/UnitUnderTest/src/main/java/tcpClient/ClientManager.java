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
import watchdog._1h_Watchdog;
import watchdog._24h_Watchdog;

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
		
		while(true)
        {
			if (isClientManagerRunning()) {
				if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null)
				{
					sensor = TCPclient.searchInClientSensorList(sensor_ID);
					if (receivedMessage instanceof ServerMessage_Request_MeasurementData) {
						if (sensor != null) {
							
							System.out.println("[ClientManager] ServerMessage_Request_MeasurementData to sensor: " + sensor.getSensorID() + " has been received.");
							System.out.println("[ClientManager] ServerMessage_Request_MeasurementData has the following timestamp: " + receivedMessage.getTimestamp());
							double pm25 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
							double pm10 = ThreadLocalRandom.current().nextDouble(0.0, 101.0);
							int humidity = ThreadLocalRandom.current().nextInt(0, 101);
							int temperature = ThreadLocalRandom.current().nextInt(0, 30);
							int pressure = ThreadLocalRandom.current().nextInt(960, 1030);
							sensor.addMeasurement(pm25, pm10, humidity, temperature, pressure);
							
							MeasurementData mes_data = sensor.readLastMeasurementData();
							System.out.println("[ClientManager] sensor: \t" + sensor.getSensorID() + " has the following number of measurements: \t"  + sensor.getNumberOfMeasurements());
							if (sensor.getNumberOfMeasurements() == 3) {
								System.out.println("[ClientManager] ack_alert for sensor: " + sensor.getSensorID() + " is set to true - the sensor waits for ServerMessage_Request_MeasurementHistory.");
								ack_alert = true;
							}
							
							sendMessage(new ClientMessage_MeasurementData(sensor_ID, mes_data));
							TCPclient.updateClientSensorList(sensor);
						}
					}
					else if (receivedMessage instanceof ServerMessage_Request_MeasurementHistory) {
						if (sensor != null) {
							
							System.out.println("[ClientManager] ServerMessage_Request_MeasurementHistory to sensor: " + sensor.getSensorID() + " has been received.");
							System.out.println("[ClientManager] ServerMessage_Request_MeasurementHistory has the following timestamp: " + receivedMessage.getTimestamp());
							MeasurementData[] mes_data = sensor.readMeasurementHistory();
							sendMessage(new ClientMessage_MeasurementHistory(sensor_ID, mes_data));
							
							// to clear the measurement history and set number of measurement to 0
							sensor.resetSensor();
							TCPclient.updateClientSensorList(sensor);
							
							ack_alert = false;
							
							break;
						}
					}
					else if (receivedMessage instanceof ServerMessage_SensorInfoQuerry) {
						if (sensor != null) {
							
							System.out.println("[ClientManager] ServerMessage_SensorInfoQuerry to sensor: " + sensor.getSensorID() + " has been received.");
							sendMessage(new ClientMessage_SensorInfo(sensor));
						}
					}
					else if (receivedMessage instanceof ServerMessage_SensorInfoUpdate) {
						if (sensor != null) {
							SensorImpl new_sensor = new SensorImpl(receivedMessage.getSensorID(), 
																((ServerMessage_SensorInfoUpdate) receivedMessage).getCoordinates(), 
																((ServerMessage_SensorInfoUpdate) receivedMessage).getSoftwareImageID());
							
							new_sensor.setSensorState(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensorState());
							
							System.out.println("[ClientManager] ServerMessage_SensorInfoUpdate to sensor: " + sensor.getSensorID() + " has been received.");
							System.out.println("[ClientManager] ServerMessage_SensorInfoUpdate has the following timestamp: " + receivedMessage.getTimestamp());
							
							if (new_sensor.getSensorState() == SensorState.MAINTENANCE) {
								new_sensor.resetSensor();
								
								// send BootUp message
						    	sendMessage(new ClientMessage_BootUp(sensor_ID));
						    	System.out.println("[ClientManager] Boot Up message send by the Client after processing ServerMessage_SensorInfoUpdate");
								
						    	// reset Sensor results in setting its state to PRE_OPERATIONAL, hence the state has to be updated to MAINTENANCE
								new_sensor.setSensorState(SensorState.MAINTENANCE);
								
							}
							else if (new_sensor.getSensorState() == SensorState.OPERATIONAL) {
								new_sensor.resetSensor();
								
								// send ACK message to disable the socket on the server side
						    	sendMessage(new ClientMessage_ACK(sensor_ID));
								
								// reset Sensor results in setting its state to PRE_OPERATIONAL, hence the state has to be updated to OPERATIONAL
								new_sensor.setSensorState(SensorState.OPERATIONAL);
							}
							else if (new_sensor.getSensorState() == SensorState.DEAD) {
								sendMessage(new ClientMessage_ACK(sensor_ID));
								setClientManagerRunning(false);
								System.out.println("[ClientManager] ServerMessage_SensorInfoUpdate has the following SensorState: " + new_sensor.getSensorState());
								System.out.println("[ClientManager] SensorState.DEAD causes the sensor ID: " + sensor_ID + " to stop communicating with the server"); 
							}
							 
							_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_SensorInfoUpdate) receivedMessage).get1h_Watchdog());
							_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_SensorInfoUpdate) receivedMessage).get24h_Watchdog());
								
							TCPclient.updateClientSensorList(new_sensor);
							
							System.out.println("[ClientManager] ServerMessage_SensorInfoUpdate has the following SensorState: " + new_sensor.getSensorState());
							System.out.println("[ClientManager] ServerMessage_SensorInfoUpdate received when _1h_Watchdog equals: " + _1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
							
							if(new_sensor.getSensorState() == SensorState.OPERATIONAL && _1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > 12) {
								System.out.println("[ClientManager] NEW CONDITION if OPERATIONAL AND STATE TO setClientManagerRunning(false) - _1h_Watchdog: " + _1h_Watchdog.getInstance().getTimeLeftBeforeExpiration());
								setClientManagerRunning(false);
							}

						}
					}
					else if (receivedMessage instanceof ServerMessage_ACK) {
						
						System.out.println("[ClientManager] ServerMessage_ACK to sensor: " + sensor.getSensorID() + " has been received.");
						System.out.println("[ClientManager] ServerMessage_ACK has the following timestamp: " + receivedMessage.getTimestamp());

						
						// ack_alert equals TRUE only if (sensor.getNumberOfMeasurements() == 24), hence setClientManagerRunning(false) has to be executed in the subsequent loop
						// upon receiving ClientMessage_MeasurementHistory
						
						if(!ack_alert) {
							setClientManagerRunning(false);
							// updating the 1h Watchdog time before expiration is required here when the sensor is in a different from the OPERATIONAL state
							_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_ACK) receivedMessage).get1h_Watchdog());
							System.out.println("[ClientManager] ClientMessage_ACK to sensor: " + sensor.getSensorID() + " has been sned that ends the server-client communication successfully");
							// send ACK message to disable the socket on the server side
					    	sendMessage(new ClientMessage_ACK(sensor_ID));
						}
						else {
							_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_ACK) receivedMessage).get24h_Watchdog());
							System.out.println("[ClientManager] ClientMessage_ACK to sensor: " + sensor.getSensorID() + " has not been send when the sensor expects the server to send the measurement history request.");
						}

					}
				}
			} 
			else {
				System.out.println("[ClientManager] is being closed for sensor ID: " + sensor_ID);
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
