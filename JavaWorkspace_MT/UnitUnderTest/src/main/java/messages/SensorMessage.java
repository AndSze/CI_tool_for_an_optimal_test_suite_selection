package messages;

public enum SensorMessage {

	SENSOR_INFO (0),
	DATA (1),
	HISTORY (2);

	private int sensorMessage;

	private SensorMessage(int sensorMessage) 
	{
		this.sensorMessage = sensorMessage;
	}

	public int getSensorMessage() 
	{
		return this.sensorMessage;
	}

	public void setSensorMessage(int sensorMessage) 
	{
		this.sensorMessage = sensorMessage;
	}
}
