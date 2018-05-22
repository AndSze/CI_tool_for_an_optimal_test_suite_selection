package tcpClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
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

public class ClientManager implements TCPclient_interface{

	private ObjectOutputStream outputStream = null;
	private ObjectInputStream inputStream = null;
	private boolean isClientManagerRunning = false;
	private int sensor_ID = 0;
	
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
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        return (new ClientManager(outputStream, inputStream, sensor_ID));
	       
	}

	public void sendMessage(Message_Interface message) throws IOException {
		
        // it activates serverSocket.accept() on the server side
		getOutputStream().writeObject(message); 
        
	}
	
	public void messagesHandler(ObjectOutputStream outputStream, ObjectInputStream inputStream) throws IOException, ClassNotFoundException, InterruptedException {
		
		Message_Interface receivedMessage = null;
		boolean ack_alert = false;
		
		while(true)
        {
			if (isClientManagerRunning()) {
				if( (receivedMessage = (Message_Interface) inputStream.readObject()) != null)
				{
					SensorImpl sensor = TCPclient.searchInClientSensorList(sensor_ID);
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
							if (sensor.getNumberOfMeasurements() == 23) {
								ack_alert = true;
							}
							
							sendMessage(new ClientMessage_MeasurementData(sensor_ID, mes_data));
							
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
						    	System.out.println("Boot Up message send by the Client after processing ServerMessage_SensorInfoUpdate");
								
						    	// reset Sensor results in setting its state to PRE_OPERATIONAL, hence the state has to be updated to MAINTENANCE
								new_sensor.setSensorState(SensorState.MAINTENANCE);
								
							}
							else if (new_sensor.getSensorState() == SensorState.OPERATIONAL) {
								new_sensor.resetSensor();
								
								// reset Sensor results in setting its state to PRE_OPERATIONAL, hence the state has to be updated to OPERATIONAL
								new_sensor.setSensorState(SensorState.OPERATIONAL);
							}
							else if (new_sensor.getSensorState() == SensorState.DEAD) {

								setClientManagerRunning(false);
								break;
							}
							
							TCPclient.updateClientSensorList(new_sensor);
							
						}
					}
					else if (receivedMessage instanceof ServerMessage_ACK) {
						
						System.out.println("[ClientManager] ServerMessage_ACK to sensor: " + sensor.getSensorID() + " has been received.");
						System.out.println("[ClientManager] ServerMessage_ACK has the following timestamp: " + receivedMessage.getTimestamp());
						if(!ack_alert) {
							setClientManagerRunning(false);
							break;
						}
					}
			        Thread.sleep(10);
				}
			} 
			else {
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
