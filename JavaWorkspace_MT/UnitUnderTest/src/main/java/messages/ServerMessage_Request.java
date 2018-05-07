package messages;

import java.io.Serializable;


public class ServerMessage_Request extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected int sensorID;
	protected ServerMessage sensorMessage;
	
	// ServerMessage_Request class constructor
	public ServerMessage_Request(int sensorID) {
		super();
		this.sensorID = sensorID;
		this.sensorMessage = ServerMessage.REQUEST;
	}

}
