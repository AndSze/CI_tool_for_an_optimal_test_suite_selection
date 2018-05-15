package messages;

import java.io.Serializable;

public class ClientMessage_BootUp extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected SensorMessage sensorMessage;
	
	// ClientMessage_BootUp class constructor
	public ClientMessage_BootUp(int sensorID) {
		super();
		this.sensorID = sensorID;
		this.sensorMessage = SensorMessage.BOOT_UP;
	}

}