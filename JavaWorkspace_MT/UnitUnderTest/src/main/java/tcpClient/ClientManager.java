package tcpClient;

import java.io.EOFException;
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

    /***********************************************************************************************************
	 * ClientManager - Class Attributes
	 ***********************************************************************************************************/
	private ObjectOutputStream outputStream = null;
	private ObjectInputStream inputStream = null;
	private boolean isClientManagerRunning = false;
	protected int sensor_ID = 0;

	/***********************************************************************************************************
	 * Method Name: 				public ClientManager()
	 * Description: 				ClientManager class default constructor
	 ***********************************************************************************************************/
	public ClientManager() {
		// to avoid calling multiple times the constructor of the TCPclient class 
		super();
	}
	
    /***********************************************************************************************************
	 * Method Name: 				ClientManager()
	 * Description: 				ClientManager class overloaded constructor
	 * Affected internal variables: outputStream, inputStream, sensor_ID, isClientManagerRunning
	 ***********************************************************************************************************/
	ClientManager(ObjectOutputStream outputStream, ObjectInputStream inputStream, int sensor_ID){
		this.outputStream = outputStream;
        this.inputStream = inputStream;
        this.sensor_ID = sensor_ID;
        isClientManagerRunning = true;
	}

    /***********************************************************************************************************
	 * Method Name: 				public ClientManager initClientManager()
	 * Description: 				Calls the default client manager class constructor with previously initialized input and output streams for a client socket
	 * Affected internal variables: outputStream, inputStream
	 * Returned value				ClientManager
	 * Called internal functions:   ClientManager()
	 * Exceptions thrown: 			IOException
	 ***********************************************************************************************************/
	public ClientManager initClientManager(Socket clientSocket, int sensor_ID) throws IOException{
		
		// it activates serverSocket.accept() on the server side
		outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        return (new ClientManager(outputStream, inputStream, sensor_ID));
	       
	}

    /***********************************************************************************************************
	 * Method Name: 				public void sendMessage()
	 * Description: 				Writes message to the output object stream
	 * Affected internal variables: outputStream
	 * Exceptions thrown: 			IOException, IllegalArgumentException
	 ***********************************************************************************************************/
	public void sendMessage(Message_Interface message, ObjectOutputStream out_stream) throws IOException {
	
		if (out_stream != null) {
			// sends message from the client via its output stream to the server input stream
			out_stream.writeObject(message); 
		}
		else {
			throw new IllegalArgumentException();
		}   
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void sendMessage()
	 * Description: 				Reads message from the input object stream
	 * Affected internal variables: inputStream
	 * Returned value:				Message_Interface
	 * Exceptions thrown: 			IOException, IllegalArgumentException, ClassNotFoundException
	 ***********************************************************************************************************/
	public Message_Interface readMessage(ObjectInputStream in_stream) throws IOException, ClassNotFoundException {
		
		Message_Interface receivedMessage = null;
				
		if (in_stream != null) {
			// reads message sent to the client input stream from the server output stream
			receivedMessage = (Message_Interface) in_stream.readObject();
		}
		else {
			throw new IllegalArgumentException();
		}   
		
		return receivedMessage;
	}
	
    /***********************************************************************************************************
	 * Method Name: 				public void messagesHandler()
	 * Description: 				State machine for massages sent to the TCP server based on received messages via TCP connection and watchdogs time left to expiration
	 * Affected internal variables: isClientManagerRunning
	 * Affected external variables: TCPclient.Client_Sensors_LIST, SensorImpl.sensorID, SensorImpl.coordinates, SensorImpl.softwareImageID, SensorImpl.sensorState,
	  								SensorImpl.sensor_m_history, Local_1h_Watchdog.millisecondsLeftUntilExpiration, Local_1h_Watchdog.isPaused, Local_1h_Watchdog.local_watchgod_scale_factor, 
	  								TCPclient.measurements_limit, TCPclient.watchdogs_scale_factor
	 * Local variables:			    sensor, receivedMessage, wait_for_measurement_history, wait_for_measurement_data
	 * Called internal functions:   sendMessage()
	 * Called external functions:   SensorImpl.addMeasurement(), SensorImpl.resetSensor(), SensorImpl(), TCPclient.updateClientSensorList(), ClientMessage_MeasurementData(), 
	 								ClientMessage_MeasurementHistory(), ClientMessage_SensorInfo(), ClientMessage_ACK(), ClientMessage_BootUp()
	 * Exceptions handled: 			IOException, ClassNotFoundException
	 ***********************************************************************************************************/
	public void messagesHandler(ObjectOutputStream outputStream, ObjectInputStream inputStream) {
		
		// local variables for used by the method
		SensorImpl sensor = null;
		Message_Interface receivedMessage = null;
		boolean wait_for_measurement_history = false;
		boolean wait_for_measurement_data = false;
		while(true)
        {
			if (isClientManagerRunning()) {
				try {
					if( (receivedMessage = readMessage(getInputReaderStream())) != null) {
						sensor = TCPclient.searchInClientSensorList(sensor_ID);
						if (receivedMessage instanceof ServerMessage_Request_MeasurementData && sensor.getSensorID() == receivedMessage.getSensorID() && wait_for_measurement_data) {

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
								wait_for_measurement_history = true;
							}
							
							sendMessage(new ClientMessage_MeasurementData(sensor_ID, mes_data), getOutputStream());
							System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_Request_MeasurementData with ClientMessage_MeasurementData.");
							
							wait_for_measurement_data = false;
							
						}
						else if (receivedMessage instanceof ServerMessage_Request_MeasurementHistory && sensor.getSensorID() == receivedMessage.getSensorID() && wait_for_measurement_history) {
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_Request_MeasurementHistory has been received.");
							System.out.println("[ClientManager " +sensor.getSensorID()+"] sensor: \t" + sensor.getSensorID() + " has the following number of measurements: \t"  + sensor.getNumberOfMeasurements());
							//System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_Request_MeasurementHistory has the following timestamp: " + receivedMessage.getTimestamp());
							MeasurementData[] mes_data = sensor.readMeasurementHistory();
							sendMessage(new ClientMessage_MeasurementHistory(sensor_ID, mes_data), getOutputStream());
							System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_Request_MeasurementHistory with ClientMessage_MeasurementHistory.");
							
							// to clear the measurement history and set number of measurement to 0
							System.out.println("[ClientManager " +sensor.getSensorID()+"] Sensor is being reset once ClientMessage_MeasurementHistory is sent");
							sensor.resetSensor();
							TCPclient.updateClientSensorList(sensor);
							
							wait_for_measurement_history = false;

						}
						else if (receivedMessage instanceof ServerMessage_SensorInfoQuerry && sensor.getSensorID() == receivedMessage.getSensorID()) {

							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_SensorInfoQuerry has been received.");
							sendMessage(new ClientMessage_SensorInfo(sensor), getOutputStream());
							System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_SensorInfoQuerry with ClientMessage_SensorInfo.");
							
						}
						else if (receivedMessage instanceof ServerMessage_SensorInfoUpdate && sensor.getSensorID() == receivedMessage.getSensorID() ) {
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
						    	sendMessage(new ClientMessage_BootUp(sensor_ID), getOutputStream());
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_SensorInfoUpdate with ClientMessage_BootUp.");
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] Sensor is being reset once ClientMessage_BootUp is being sent");
						    	// reset Sensor results in setting its state to PRE_OPERATIONAL, hence the state has to be updated to MAINTENANCE
						    	if (previousSensorState == SensorState.MAINTENANCE) {
						    		new_sensor.setSensorState(SensorState.MAINTENANCE);
						    	}
								TCPclient.updateClientSensorList(new_sensor);
								
							}
							else if (new_sensor.getSensorState() == SensorState.OPERATIONAL) {
								
								// send ACK message to indicate that the configuration is successful 
						    	sendMessage(new ClientMessage_ACK(sensor_ID), getOutputStream());
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_SensorInfoUpdate with ClientMessage_ACK.");
						    	
						    	sensor.setSensorState(SensorState.OPERATIONAL);
						    	sensor.setSensor_watchdog_scale_factor(((ServerMessage_SensorInfoUpdate) receivedMessage).getSensor_watchdog_scale_factor());

						    	TCPclient.updateClientSensorList(sensor);
								
						    	wait_for_measurement_data = true;
							}
							else if (new_sensor.getSensorState() == SensorState.DEAD) {
								sendMessage(new ClientMessage_ACK(sensor_ID), getOutputStream());
								System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_SensorInfoUpdate with ClientMessage_ACK.");
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

							
							if(new_sensor.getSensorState() == SensorState.OPERATIONAL && Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() > ( (0.08333333 * Local_1h_Watchdog.getInstance().getExpiration()) * ((ServerMessage_SensorInfoUpdate) receivedMessage).getSensor_watchdog_scale_factor()) ) {
								System.out.println("[ClientManager " +sensor.getSensorID()+"] if sensor receives go to OPERATIONAL message, ClientManager is being closed since its Local_1h_Watchdog is not close to expire"); 
								System.out.println("[ClientManager " +sensor.getSensorID()+"] it will be launched agan once its Local_1h_Watchdog: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + "[s] will reach threshold for launching the TCP connection.");
								setClientManagerRunning(false);
							}

						}
						else if (receivedMessage instanceof ServerMessage_ACK && sensor.getSensorID() == receivedMessage.getSensorID() ) {
							
							System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_ACK has been received.");
							//System.out.println("[ClientManager " +sensor.getSensorID()+"] ServerMessage_ACK has the following timestamp: " + receivedMessage.getTimestamp());

							
							// ack_alert equals TRUE only if (sensor.getNumberOfMeasurements() == 24), hence setClientManagerRunning(false) has to be executed in the subsequent loop
							// upon receiving ClientMessage_MeasurementHistory
							
							if(!wait_for_measurement_history) {
								setClientManagerRunning(false);
								// updating the 1h Watchdog time before expiration is required here when the sensor is in a different from the OPERATIONAL state
								Local_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(((ServerMessage_ACK) receivedMessage).get1h_Watchdog());
								System.out.println("[ClientManager " +sensor.getSensorID()+"] Local_1h_Watchdog for the sensor has been synchronized and it equals: " + Local_1h_Watchdog.getInstance().getTimeLeftBeforeExpiration() + " [s]" );
								// send ACK message to disable the socket on the server side
						    	sendMessage(new ClientMessage_ACK(sensor_ID), getOutputStream());
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_ACK with ClientMessage_ACK to end the server-client connection successfully.");
						    	
						    	// enable Local watchdog if state machine reaches this condition after sending measurement history 
						    	if(Local_1h_Watchdog.getInstance().getEnabled() == false) {
						    		Local_1h_Watchdog.getInstance().setEnabled(true);
						    	}
							}
							else {
								// send ACK message to confirm that ClientMessage_MeasurementData has been sent, but do not disable the socket on the server side - the sensor waits for ServerMessage_Request_MeasurementHistory
								sendMessage(new ClientMessage_ACK(sensor_ID), getOutputStream());
						    	System.out.println("[ClientManager " +sensor.getSensorID()+"] responds to ServerMessage_ACK with ClientMessage_ACK to confirm that ClientMessage_MeasurementData has been sent - wait for ServerMessage_Request_MeasurementHistory.");
								
								// disable Local watchdog if state if measurement history is going to be send
								Local_1h_Watchdog.getInstance().setEnabled(false);
							}
						}
						else if(sensor.getSensorID() != receivedMessage.getSensorID()) {
							System.out.println("[ClientManager " +sensor.getSensorID()+"] message with invalid sensor ID has been received.");
							
							sensor.setSensorID(receivedMessage.getSensorID());
							TCPclient.updateClientSensorList(sensor);
							
							sensor_ID = receivedMessage.getSensorID();
							
							sendMessage(new ClientMessage_SensorInfo(sensor), getOutputStream());
						}
					}
				} catch (ClassNotFoundException CNFex) {
		            System.out.println("[ClientManager " +sensor.getSensorID()+"] Error: readMessage() failed due to class of a deserialized object cannot be found");
		        	System.out.println(CNFex.getMessage());
		        	setClientManagerRunning(false);
				} catch (EOFException EOFex) {
					System.out.println("[ClientManager " +sensor.getSensorID()+"] Error: readMessage() failed due the fact that Global_1h_Watchdog on the server side has expired and readMessage() cannot receive any new messages");
			        System.out.println(EOFex.getMessage());
			        setClientManagerRunning(false);
				} catch (IOException IOex) {
					System.out.println("[ClientManager " +sensor.getSensorID()+"] Error: readMessage() failed due to TCP connection issues - when attempted to read Object from inputStream on the client side");
			        System.out.println(IOex.getMessage());
			        setClientManagerRunning(false);
				}
			} 
			else {
				System.out.println("[ClientManager " +sensor.getSensorID()+"] is being closed for sensor ID: " + sensor_ID);
				break;
			}
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
	 * Auxiliary piece of code
	 * Description: 				getters & setters for class attributes			
	 ***********************************************************************************************************/
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
	
    public void setSensor_ID(int sensor_ID) {
		this.sensor_ID = sensor_ID;
	}
	
}
