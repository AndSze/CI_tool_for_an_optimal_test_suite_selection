package messages;

public enum SensorMessage {

	DATA (0),
	ACK (1),
	REQUEST (2),
	SENSOR_INFO (3);

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
