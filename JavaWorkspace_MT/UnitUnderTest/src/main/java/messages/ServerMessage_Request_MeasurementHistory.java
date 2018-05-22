package messages;

import java.io.Serializable;

public class ServerMessage_Request_MeasurementHistory extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected ServerMessage serverMessage;
	
	// ServerMessage_Request_MeasurementHistory class constructor
	public ServerMessage_Request_MeasurementHistory(int sensorID) {
		super(sensorID);
		this.serverMessage = ServerMessage.REQUEST_MEASUREMENT_HISOTRY;
	}

}
