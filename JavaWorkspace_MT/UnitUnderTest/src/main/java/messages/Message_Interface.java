package messages;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public abstract class Message_Interface extends Interface{
	
	// class attributes
	protected String timestamp;

	// ClientMessage_Interface class constructor
	public Message_Interface() {
		this.timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
	}
	
	public void sendMessage() {
	}
	
}
