package tcpClient;

public enum Delays {

	LOWEST (0),
	// Local 1h Watchdog is very close to expire
	LOW (1),
	// Local 1h Watchdog is close to expire
	MEDIUM (2),
	// Local 1h Watchdog is not close to expire
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

