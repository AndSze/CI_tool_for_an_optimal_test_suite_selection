package messages;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Message_Interface extends Message{
	
	// class attributes
	protected String timestamp;
	protected int sensorID;

	// ClientMessage_Interface class constructor
	public Message_Interface() {
		this.timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}
	
	public int getSensorID() {
		return sensorID;
	}

	public void setSensorID(int sensorID) {
		this.sensorID = sensorID;
	}
	
}
