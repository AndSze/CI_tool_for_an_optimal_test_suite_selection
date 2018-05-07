package messages;

public enum SensorRequest {
	
	NORMAL (0),
	ABNORMAL (1);

	private int sensorRequest;

	private SensorRequest(int sensorRequest) 
	{
		this.sensorRequest = sensorRequest;
	}

	public int getSensorRequest() 
	{
		return this.sensorRequest;
	}

	public void setSensorRequest(int sensorRequest) 
	{
		this.sensorRequest = sensorRequest;
	}

}
