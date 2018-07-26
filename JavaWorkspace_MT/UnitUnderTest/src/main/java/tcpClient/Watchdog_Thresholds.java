package tcpClient;

public enum Watchdog_Thresholds {

	// send measurement data request
	LOWEST (0),
	// decrease processing delays between consecutive Local 1h Watchdog time to expiration read
	MEDIUM (1),
	// do not decrease processing delays between consecutive Local 1h Watchdog time to expiration read
	HIGH (2),
	// do not decrease processing delays between consecutive Local 1h Watchdog time to expiration read
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

