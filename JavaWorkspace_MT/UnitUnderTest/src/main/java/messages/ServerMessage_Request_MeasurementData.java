package messages;

import java.io.Serializable;


public class ServerMessage_Request_MeasurementData extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected ServerMessage serverMessage;
	
	// ServerMessage_Request_MeasurementData class constructor
	public ServerMessage_Request_MeasurementData(int sensorID) {
		super();
		this.sensorID = sensorID;
		this.serverMessage = ServerMessage.REQUEST_MEASUREMENT_DATA;
	}

}
