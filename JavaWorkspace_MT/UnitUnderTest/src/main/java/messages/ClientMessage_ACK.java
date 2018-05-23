package messages;

import java.io.Serializable;

public class ClientMessage_ACK extends Message_Interface implements Serializable {

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected SensorMessage sensorMessage;
	
	// ClientMessage_ACK class constructor
	public ClientMessage_ACK(int sensorID) {
		super(sensorID);
		this.sensorMessage = SensorMessage.BOOT_UP;
	}

}