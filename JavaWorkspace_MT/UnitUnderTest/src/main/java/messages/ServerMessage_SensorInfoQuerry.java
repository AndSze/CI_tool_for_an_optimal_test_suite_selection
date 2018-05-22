package messages;

import java.io.Serializable;

public class ServerMessage_SensorInfoQuerry extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected ServerMessage sensorMessage;
	
	// ServerMessage_SensorInfoQuerry class constructor
	public ServerMessage_SensorInfoQuerry(int sensorID) {
		super(sensorID);
		this.sensorMessage = ServerMessage.SENSOR_INFO_QUERY;
	}

}
