package messages;

public enum ServerMessage {
	
	/* every 24h sends REQUEST_MEASUREMENT_HISOTRY, then sends SENSOR_INFO_QUERY, then sends SENSOR_INFO_UPDATE,  
	 * if MEASUREMENT_HISOTRY and SENSOR_INFO are the same as on the server side, then the sensor reset is triggered (MEASUREMENT_HISOTRY is cleared)
	 */
	REQUEST_MEASUREMENT_DATA (0),
	REQUEST_MEASUREMENT_HISOTRY (1),
	ACK (2),
	SENSOR_INFO_QUERY (3),
	SENSOR_INFO_UPDATE (4);

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
