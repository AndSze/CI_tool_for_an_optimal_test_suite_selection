package watchdog;

import java.util.concurrent.locks.Lock; 
import java.util.concurrent.locks.ReentrantLock;

import tcpServer.TCPserver;

/*
 *  This file is part of frcjcss. 
 * 
 *  frcjcss is free software: you can redistribute it and/or modify 
 *  it under the terms of the GNU General Public License as published by 
 *  the Free Software Foundation, either version 3 of the License, or 
 *  (at your option) any later version. 
 * 
 *  frcjcss is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU General Public License for more details. 
 * 
 *  You should have received a copy of the GNU General Public License 
 *  along with frcjcss.  If not, see <http://www.gnu.org/licenses/>. 
 */ 

/*
 * _1h_Watchdog timer class. 
 * The _1h_Watchdog timer is designed to keep the robots safe. The idea is that the robot program must 
 * constantly "feed" the _1h_Watchdog otherwise it will shut down all the motor outputs. That way if a 
 * program breaks, rather than having the robot continue to operate at the last known speed, the 
 * motors will be shut down. 
 * 
 * This is serious business.  Don't just disable the _1h_Watchdog.  You can't afford it! 
 * 
 * http://thedailywtf.com/Articles/_0x2f__0x2f_TODO_0x3a__Uncomment_Later.aspx 
 */ 

public class Global_1h_Watchdog implements Runnable { 
 
    private static Global_1h_Watchdog m_instance; 
	private double millisecondsLeftUntilExpiration; 
    private Thread _1h_WatchdogThread; 
    private Lock expirationDateLock; 
    // _1h_Watchdog expiration time is given in seconds
    private final int _1h_WatchdogExpiration = 3600; 
    // _1h_Watchdog expiration time decrementation timeIntervals in milliseconds (its value is decremented every second)
    private int timeIntervals = 100; 
	private boolean isPaused = true; 
	private static double server_watchgod_scale_factor = 1.0;

	/*
     * The _1h_Watchdog is born. 
     */ 
    protected Global_1h_Watchdog(double global_watchdogs_scale_factor) {            
        expirationDateLock = new ReentrantLock(); 
        setServer_watchgod_scale_factor(global_watchdogs_scale_factor);
        if(global_watchdogs_scale_factor > 0.01) {
        	setTimeIntervals( (int) (getTimeIntervals() * global_watchdogs_scale_factor));
        }
        else {
        	setTimeIntervals( (int) (getTimeIntervals() * 0.01));
        }
        millisecondsLeftUntilExpiration = (double) (global_watchdogs_scale_factor * (_1h_WatchdogExpiration*1000)); 
        _1h_WatchdogThread = new Thread(this, "_1h_Watchdog Thread"); 
        _1h_WatchdogThread.start(); 
    } 
 
    /*
     *  Get an instance of the _1h_Watchdog 
     * @return an instance of the _1h_Watchdog 
     */ 
    public static synchronized Global_1h_Watchdog getInstance() { 
        if (m_instance == null) { 
            m_instance = new Global_1h_Watchdog(TCPserver.getWatchdogs_scale_factor()); 
        } 
        return m_instance; 
    } 
 
    /*
     * Throw the dog a bone. 
     * 
     * When everything is going well, you feed your dog when you get home. 
     * Let's hope you don't drive your car off a bridge on the way home... 
     * Your dog won't get fed and he will starve to death. 
     * 
     * By the way, it's not cool to ask the neighbor (some random task) to 
     * feed your dog for you.  He's your responsibility! 
     */ 
    public synchronized void feed() { 
    	if (!((ReentrantLock) expirationDateLock).isLocked()) {
    		expirationDateLock.lock(); 
    		millisecondsLeftUntilExpiration = (double) (getServer_watchgod_scale_factor() * _1h_WatchdogExpiration * 1000 ); 
    		expirationDateLock.unlock(); 
    	}
    } 
 
    /*
     * Put the _1h_Watchdog out of its misery. 
     * 
     * Don't wait for your dying robot to starve when there is a problem. 
     * Kill it quickly, cleanly, and humanely. 
     */ 
    public void kill() { 
    	_1h_WatchdogThread.interrupt(); 
    } 
 
    /*
     * Read the remaining time for the _1h_Watchdog unless it will be fed
     * 
     * @return The number of seconds left to _1h_Watchdog expiration. 
     */ 
    public double getTimeLeftBeforeExpiration() { 
        return millisecondsLeftUntilExpiration * 0.001;  
    } 
    
    /*
     * Set the remaining time for the _1h_Watchdog 
     * It should be called to update _1h_Watchdog on the client side respectively to the _1h_Watchdog
     */ 
    public void setTimeLeftBeforeExpiration(double _1h_WatchdogExpiration) { 
    	 millisecondsLeftUntilExpiration = (long) (_1h_WatchdogExpiration * 1000); 
    } 
    
    /*
     * Read how long it has been since the _1h_Watchdog was last fed. 
     * 
     * @return The number of seconds since last meal. 
     */ 
    public synchronized double getTimeFromLastFeed() { 
        return (_1h_WatchdogExpiration - (millisecondsLeftUntilExpiration * 0.001)); 
    } 
 
    /*
     * Read what the current expiration is. 
     * 
     * @return The number of seconds before starvation following a meal (_1h_Watchdog starves if it doesn't eat this often). 
     */ 
    public int getExpiration() { 
        return _1h_WatchdogExpiration; 
    } 

    /*
     * Find out if the _1h_Watchdog is currently enabled or disabled (mortal or immortal). 
     * 
     * @return Enabled or disabled. 
     */ 
    public boolean getEnabled() { 
        return !isPaused; 
    } 
 
    /*
     * Enable or disable the _1h_Watchdog timer. 
     * 
     * When enabled, you must keep feeding the _1h_Watchdog timer to 
     * keep the _1h_Watchdog active, and hence the dangerous parts 
     * (motor outputs, etc.) can keep functioning. 
     * When disabled, the _1h_Watchdog is immortal and will remain active 
     * even without being fed.  It will also ignore any kill commands 
     * while disabled. 
     * 
     * @param enabled Enable or disable the _1h_Watchdog. 
     */ 
    public synchronized void setEnabled(final boolean enabled) { 
    	if (!((ReentrantLock) expirationDateLock).isLocked()) {
			expirationDateLock.lock(); 
			isPaused = !enabled; 
			expirationDateLock.unlock(); 
    	}
    } 
 
    /*
     * Check in on the _1h_Watchdog and make sure he's still kicking. 
     * 
     * This indicates that your _1h_Watchdog is allowing the system to operate. 
     * It is still possible that the network communications is not allowing the 
     * system to run, but you can check this to make sure it's not your fault. 
     * Check isSystemActive() for overall system status. 
     * 
     * If the _1h_Watchdog is disabled, then your _1h_Watchdog is immortal. 
     * 
     * @return Is the _1h_Watchdog still alive? 
     */ 
    public boolean isAlive() { 
    	return _1h_WatchdogThread.isAlive(); 
    } 
 
    /*
     * Check on the overall status of the system. 
     * 
     * @return Is the system active (i.e. PWM motor outputs, etc. enabled)? 
     */ 
    public boolean isSystemActive() { 
        return true; 
    } 
	 
    /*
     * _1h_Watchdog is decremented every 1000 * global_watchdogs_scale_factor [milliseconds]
     */ 
    public void run() { 
    	while(millisecondsLeftUntilExpiration > 0) { 
    		try { 
    			expirationDateLock.lock(); 
    			if(!isPaused) { 
    				millisecondsLeftUntilExpiration -= getTimeIntervals(); 
    			} 
    			expirationDateLock.unlock(); 
    			Thread.sleep(getTimeIntervals()); 
    		} catch(InterruptedException IntEx) {
    			System.out.println("Error: when attempted to interrupt the _1h_Watchdog when the thread is waiting, sleeping, or otherwise occupied");
	            System.out.println(IntEx.getMessage());
    			break; 
    		} 
    	} 
    }
    
    public int getTimeIntervals() {
		return timeIntervals;
	}

	public void setTimeIntervals(int timeIntervals) {
		this.timeIntervals = timeIntervals;
	}
	 
    public static double getServer_watchgod_scale_factor() {
		return server_watchgod_scale_factor;
	}

	public void setServer_watchgod_scale_factor(double global_watchgod_scale_factor) {
		server_watchgod_scale_factor = global_watchgod_scale_factor;
	}
	
    public static void setM_instance(Global_1h_Watchdog m_instance) {
		Global_1h_Watchdog.m_instance = m_instance;
	}

}
