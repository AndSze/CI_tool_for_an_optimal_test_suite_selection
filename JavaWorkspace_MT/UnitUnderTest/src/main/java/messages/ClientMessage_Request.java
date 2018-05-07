package messages;

import java.io.Serializable;


public class ClientMessage_Request extends Message_Interface implements Serializable{
	
	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected int sensorID;
	protected SensorMessage sensorMessage;
	protected SensorRequest sensorRequest;
	
	// ClientMessage_Request class constructor
	public ClientMessage_Request(int sensorID, SensorRequest sensorRequest) {
		super();
		this.sensorID = sensorID;
		this.sensorMessage = SensorMessage.REQUEST;
		this.sensorRequest = sensorRequest;
	}

}
