package messages;

public enum ServerMessage {

	REQUEST (0),
	ACK (1),
	SENSOR_INFO_QUERY (2),
	SENSOR_INFO_UPDATE (3);

	private int serverMessage;

	private ServerMessage(int serverMessage) 
	{
		this.serverMessage = serverMessage;
	}

	public int getServerMessage() 
	{
		return this.serverMessage;
	}

	public void setServerMessage(int serverMessage) 
	{
		this.serverMessage = serverMessage;
	}
	

}
