package tcpServer;

public enum Delays {

	LOWEST (0),
	// send measurement data / history request
	LOW (1),
	// decrease time left before expiration twice
	MEDIUM (2),
	// enter logic for measurements request
	HIGHEST (3);

	private int delay;

	private Delays(int delay) 
	{
		this.delay = delay;
	}

	public int getDelays() 
	{
		return this.delay;
	}

	public void setDelays(int delay) 
	{
		this.delay = delay;
	}
}
