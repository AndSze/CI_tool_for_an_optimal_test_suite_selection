package tcpClient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
import messages.ClientMessage_MeasurementData;
import messages.ClientMessage_MeasurementHistory;
import messages.ClientMessage_SensorInfo;
import messages.Message_Interface;
import messages.ServerMessage_ACK;
import messages.ServerMessage_Request_MeasurementData;
import messages.ServerMessage_Request_MeasurementHistory;
import messages.ServerMessage_SensorInfoQuerry;
import messages.ServerMessage_SensorInfoUpdate;
import sensor.MeasurementData;
import sensor.SensorImpl;

public class ClientManager extends TCPclient implements TCPclient_interface{

	private ObjectOutputStream outputStream = null;
	private ObjectInputStream inputStream = null;
	private boolean isClientManagerRunning = false;
	
	// default constructor 
	public ClientManager() {
		super();
	}
	
	// overloaded constructor
	private ClientManager(ObjectOutputStream outputStream, ObjectInputStream inputStream){
		this.outputStream = outputStream;
        this.inputStream = inputStream;
        isClientManagerRunning = true;
	}



	public ClientManager initClientManager(Socket clientSocket) throws IOException{
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        return (new ClientManager(outputStream, inputStream));
	       
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
					SensorImpl sensor = searchInClientSensorList(getSensor_ID());
					if (receivedMessage instanceof ServerMessage_Request_MeasurementData) {
						if (sensor != null) {
							
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
							
							sendMessage(new ClientMessage_MeasurementData(getSensor_ID(), mes_data));
							
						}
					}
					else if (receivedMessage instanceof ServerMessage_Request_MeasurementHistory) {
						if (sensor != null) {
							
							MeasurementData[] mes_data = sensor.readMeasurementHistory();
							sendMessage(new ClientMessage_MeasurementHistory(getSensor_ID(), mes_data));
							
							// to clear the measurement history and set number of measurement to 0
							sensor.resetSensor();
							updateClientSensorList(sensor);
							
							ack_alert = false;
							
							break;
						}
					}
					else if (receivedMessage instanceof ServerMessage_SensorInfoQuerry) {
						if (sensor != null) {
							sendMessage(new ClientMessage_SensorInfo(sensor));
						}
					}
					else if (receivedMessage instanceof ServerMessage_SensorInfoUpdate) {
						if (sensor != null) {
							SensorImpl new_sensor = new SensorImpl(receivedMessage.getSensorID(), 
																((ServerMessage_SensorInfoUpdate) receivedMessage).getCoordinates(), 
																((ServerMessage_SensorInfoUpdate) receivedMessage).getSoftwareImageID());
							updateClientSensorList(new_sensor);
							new_sensor.resetSensor();
							
							new_sensor.setSensorState(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensorState());
							
							setClientManagerRunning(false);
							break;
						}
					}
					else if (receivedMessage instanceof ServerMessage_ACK) {
						if(!ack_alert) {
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
