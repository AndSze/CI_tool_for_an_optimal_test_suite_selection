package messages;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Message_Interface extends Message implements Serializable {
	
	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected String timestamp;
	protected int sensorID;

	// ClientMessage_Interface class constructor
	public Message_Interface(int sensorID) {
		this.sensorID = sensorID;
		this.timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}
	
	public int getSensorID() {
		return sensorID;
	}

	public void setSensorID(int sensorID) {
		this.sensorID = sensorID;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
}
