package messages;

import java.io.Serializable;

public class ServerMessage_ACK extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected int sensorID;
	protected ServerMessage sensorMessage;
	protected ServerACKType ack_type;
	
	// ServerMessage_ACK class constructor
	public ServerMessage_ACK(int sensorID, ServerACKType ack_type) {
		super();
		this.sensorID = sensorID;
		this.sensorMessage = ServerMessage.ACK;
		this.ack_type = ack_type;
	}

}
