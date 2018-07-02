package tcpServer;

public enum Watchdog_Thresholds {

	// send measurement data / history request
	LOWEST (0),
	// decrease time left before expiration twice
	MEDIUM (1),
	// enter logic for measurements data request
	HIGH (2),
	// enter logic for measurements history request
	HIGHEST (3);
	
	private int watchdogThreashold;

	private Watchdog_Thresholds(int watchdogThreashold) 
	{
		this.watchdogThreashold = watchdogThreashold;
	}

	public int getWatchdog_Thresholds() 
	{
		return this.watchdogThreashold;
	}

	public void setWatchdog_Thresholds(int watchdogThreashold) 
	{
		this.watchdogThreashold = watchdogThreashold;
	}
}
