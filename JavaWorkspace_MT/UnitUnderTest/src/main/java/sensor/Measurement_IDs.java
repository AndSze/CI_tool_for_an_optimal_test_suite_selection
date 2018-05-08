package sensor;

public enum Measurement_IDs {
	
	PM25 (0),
	PM10 (1),
	HUMIDITY (2),
	TEMPERATURE (3),
	PRESSURE (4);

	private int measurement_ID;

	private Measurement_IDs(int measurement_ID) 
	{
		this.measurement_ID = measurement_ID;
	}

	public int getMeasurement_IDs() 
	{
		return this.measurement_ID;
	}

	public void setMeasurement_IDs(int measurement_ID) 
	{
		this.measurement_ID = measurement_ID;
	}

}
