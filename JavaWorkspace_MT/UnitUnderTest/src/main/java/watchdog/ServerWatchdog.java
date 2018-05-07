package watchdog;

import java.util.concurrent.locks.Lock; 
import java.util.concurrent.locks.ReentrantLock;

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
 * serverWatchdog timer class. 
 * The serverWatchdog timer is designed to keep the robots safe. The idea is that the robot program must 
 * constantly "feed" the serverWatchdog otherwise it will shut down all the motor outputs. That way if a 
 * program breaks, rather than having the robot continue to operate at the last known speed, the 
 * motors will be shut down. 
 * 
 * This is serious business.  Don't just disable the serverWatchdog.  You can't afford it! 
 * 
 * http://thedailywtf.com/Articles/_0x2f__0x2f_TODO_0x3a__Uncomment_Later.aspx 
 */ 

public class ServerWatchdog implements Runnable { 
 
    private static ServerWatchdog m_instance; 
    private long millisecondsLeftUntilExpiration; 
    private Thread serverWatchdogThread; 
    private Lock expirationDateLock; 
    // ServerWatchdog expiration time is given in seconds
    private double serverWatchdogExpiration = 100; 
    // ServerWatchdog expiration time decrementation timeIntervals in milliseconds
    private int timeIntervals = 100; 
    private boolean isPaused = false; 
 
    /*
     * The ServerserverWatchdog is born. 
     */ 
    protected ServerWatchdog() {            
        expirationDateLock = new ReentrantLock(); 
        millisecondsLeftUntilExpiration = (long) (serverWatchdogExpiration*1000); 
        serverWatchdogThread = new Thread(this, "ServerWatchdog Thread"); 
        serverWatchdogThread.start(); 
    } 
 
    /*
     *  Get an instance of the ServerserverWatchdog 
     * @return an instance of the ServerserverWatchdog 
     */ 
    public static synchronized ServerWatchdog getInstance() { 
        if (m_instance == null) { 
            m_instance = new ServerWatchdog(); 
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
    public void feed() { 
		expirationDateLock.lock(); 
		millisecondsLeftUntilExpiration = (long) (serverWatchdogExpiration * 1000); 
		expirationDateLock.unlock(); 
    } 
 
    /*
     * Put the ServerserverWatchdog out of its misery. 
     * 
     * Don't wait for your dying robot to starve when there is a problem. 
     * Kill it quickly, cleanly, and humanely. 
     */ 
    public void kill() { 
    	serverWatchdogThread.interrupt(); 
    } 
 
    /*
     * Read the remaining time for the ServerWatchgod unless it will be fed
     * 
     * @return The number of seconds left to ServerWatchgod expiration. 
     */ 
    public double getTimeLeftBeforeExpiration() { 
        return millisecondsLeftUntilExpiration / 1000.0; 
    } 
    
    /*
     * Read how long it has been since the serverWatchdog was last fed. 
     * 
     * @return The number of seconds since last meal. 
     */ 
    public double getTimeFromLastFeed() { 
        return (serverWatchdogExpiration - (millisecondsLeftUntilExpiration / 1000.0)); 
    } 
 
    /*
     * Read what the current expiration is. 
     * 
     * @return The number of seconds before starvation following a meal (serverWatchdog starves if it doesn't eat this often). 
     */ 
    public double getExpiration() { 
        return serverWatchdogExpiration; 
    } 

    /*
     * Configure how many seconds your serverWatchdog can be neglected before it starves to death. 
     * 
     * @param expiration The number of seconds before starvation following a meal (serverWatchdog starves if it doesn't eat this often). 
     */ 
    public void setExpiration(double expiration) { 
        serverWatchdogExpiration = expiration; 
    } 
 
    /*
     * Find out if the serverWatchdog is currently enabled or disabled (mortal or immortal). 
     * 
     * @return Enabled or disabled. 
     */ 
    public boolean getEnabled() { 
        return !isPaused; 
    } 
 
    /*
     * Enable or disable the serverWatchdog timer. 
     * 
     * When enabled, you must keep feeding the serverWatchdog timer to 
     * keep the serverWatchdog active, and hence the dangerous parts 
     * (motor outputs, etc.) can keep functioning. 
     * When disabled, the serverWatchdog is immortal and will remain active 
     * even without being fed.  It will also ignore any kill commands 
     * while disabled. 
     * 
     * @param enabled Enable or disable the serverWatchdog. 
     */ 
    public void setEnabled(final boolean enabled) { 
		expirationDateLock.lock(); 
		isPaused = !enabled; 
		expirationDateLock.unlock(); 
    } 
 
    /*
     * Check in on the serverWatchdog and make sure he's still kicking. 
     * 
     * This indicates that your serverWatchdog is allowing the system to operate. 
     * It is still possible that the network communications is not allowing the 
     * system to run, but you can check this to make sure it's not your fault. 
     * Check isSystemActive() for overall system status. 
     * 
     * If the serverWatchdog is disabled, then your serverWatchdog is immortal. 
     * 
     * @return Is the serverWatchdog still alive? 
     */ 
    public boolean isAlive() { 
    	return serverWatchdogThread.isAlive(); 
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
     * ServerWatchdog is decremented every 100 milliseconds
     */ 
    public void run() { 
    	while(millisecondsLeftUntilExpiration > 0) { 
    		try { 
    			expirationDateLock.lock(); 
    			if(!isPaused) { 
    				millisecondsLeftUntilExpiration -= timeIntervals; 
    			} 
    			expirationDateLock.unlock(); 
    			Thread.sleep(timeIntervals); 
    		} catch(InterruptedException IntEx) { 
    			System.out.println("Error: when attempted to interrupt the ServerWatchdog when the thread is waiting, sleeping, or otherwise occupied");
	            System.out.println(IntEx.getMessage());
	            break;
    		} 
    	} 
    }
    
}
