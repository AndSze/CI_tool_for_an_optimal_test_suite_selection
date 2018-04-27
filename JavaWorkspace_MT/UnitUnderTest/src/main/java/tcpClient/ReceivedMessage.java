package tcpClient;

public class ReceivedMessage {

	private String message;
	private long timestamp;
	
	public ReceivedMessage(String message, long timestamp) {
		this.setMessage(message);
		this.setTimestamp(timestamp);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
