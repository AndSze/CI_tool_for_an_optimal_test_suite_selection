package messages;

public enum SensorState {

	OPERATIONAL (0),
	PRE_OPERATIONAL (1),
	MAINTENANCE (2),
	DEAD (3);

	private int state;

	private SensorState(int state) 
	{
		this.state = state;
	}

	public int getSensorStat() 
	{
		return this.state;
	}

	public void setSensorStat(int state) 
	{
		this.state = state;
	}
}
