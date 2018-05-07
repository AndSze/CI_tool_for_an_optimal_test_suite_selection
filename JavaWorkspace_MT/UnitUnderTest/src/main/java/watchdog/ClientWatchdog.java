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
 * clientWatchdog timer class. 
 * The clientWatchdog timer is designed to keep the robots safe. The idea is that the robot program must 
 * constantly "feed" the clientWatchdog otherwise it will shut down all the motor outputs. That way if a 
 * program breaks, rather than having the robot continue to operate at the last known speed, the 
 * motors will be shut down. 
 * 
 * This is serious business.  Don't just disable the clientWatchdog.  You can't afford it! 
 * 
 * http://thedailywtf.com/Articles/_0x2f__0x2f_TODO_0x3a__Uncomment_Later.aspx 
 */ 

public class ClientWatchdog implements Runnable { 
 
    private static ClientWatchdog m_instance; 
    private long millisecondsLeftUntilExpiration; 
    private Thread clientWatchdogThread; 
    private Lock expirationDateLock; 
    // ClientWatchdog expiration time is given in seconds
    private double clientWatchdogExpiration = 65; 
    // ClientWatchdog expiration time decrementation timeIntervals in milliseconds
    private int timeIntervals = 100; 
    private boolean isPaused = false; 
 
    /*
     * The ClientWatchdog is born. 
     */ 
    protected ClientWatchdog() {            
        expirationDateLock = new ReentrantLock(); 
        millisecondsLeftUntilExpiration = (long) (clientWatchdogExpiration*1000); 
        clientWatchdogThread = new Thread(this, "ClientWatchdog Thread"); 
        clientWatchdogThread.start(); 
    } 
 
    /*
     *  Get an instance of the ClientWatchdog 
     * @return an instance of the ClientWatchdog 
     */ 
    public static synchronized ClientWatchdog getInstance() { 
        if (m_instance == null) { 
            m_instance = new ClientWatchdog(); 
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
		millisecondsLeftUntilExpiration = (long) (clientWatchdogExpiration * 1000); 
		expirationDateLock.unlock(); 
    } 
 
    /*
     * Put the ClientWatchdog out of its misery. 
     * 
     * Don't wait for your dying robot to starve when there is a problem. 
     * Kill it quickly, cleanly, and humanely. 
     */ 
    public void kill() { 
    	clientWatchdogThread.interrupt(); 
    } 
 
    /*
     * Read the remaining time for the ClientWatchdog unless it will be fed
     * 
     * @return The number of seconds left to ClientWatchdog expiration. 
     */ 
    public double getTimeLeftBeforeExpiration() { 
        return millisecondsLeftUntilExpiration / 1000.0; 
    } 
    
    /*
     * Read how long it has been since the ClientWatchdog was last fed. 
     * 
     * @return The number of seconds since last meal. 
     */ 
    public double getTimeFromLastFeed() { 
        return (clientWatchdogExpiration - (millisecondsLeftUntilExpiration / 1000.0)); 
    } 
 
    /*
     * Read what the current expiration is. 
     * 
     * @return The number of seconds before starvation following a meal (clientWatchdog starves if it doesn't eat this often). 
     */ 
    public double getExpiration() { 
        return clientWatchdogExpiration; 
    } 

    /*
     * Configure how many seconds your clientWatchdog can be neglected before it starves to death. 
     * 
     * @param expiration The number of seconds before starvation following a meal (clientWatchdog starves if it doesn't eat this often). 
     */ 
    public void setExpiration(double expiration) { 
        clientWatchdogExpiration = expiration; 
    } 
 
    /*
     * Find out if the clientWatchdog is currently enabled or disabled (mortal or immortal). 
     * 
     * @return Enabled or disabled. 
     */ 
    public boolean getEnabled() { 
        return !isPaused; 
    } 
 
    /*
     * Enable or disable the clientWatchdog timer. 
     * 
     * When enabled, you must keep feeding the clientWatchdog timer to 
     * keep the clientWatchdog active, and hence the dangerous parts 
     * (motor outputs, etc.) can keep functioning. 
     * When disabled, the clientWatchdog is immortal and will remain active 
     * even without being fed.  It will also ignore any kill commands 
     * while disabled. 
     * 
     * @param enabled Enable or disable the clientWatchdog. 
     */ 
    public void setEnabled(final boolean enabled) { 
		expirationDateLock.lock(); 
		isPaused = !enabled; 
		expirationDateLock.unlock(); 
    } 
 
    /*
     * Check in on the clientWatchdog and make sure he's still kicking. 
     * 
     * This indicates that your clientWatchdog is allowing the system to operate. 
     * It is still possible that the network communications is not allowing the 
     * system to run, but you can check this to make sure it's not your fault. 
     * Check isSystemActive() for overall system status. 
     * 
     * If the clientWatchdog is disabled, then your clientWatchdog is immortal. 
     * 
     * @return Is the clientWatchdog still alive? 
     */ 
    public boolean isAlive() { 
    	return clientWatchdogThread.isAlive(); 
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
     * ClientWatchdog is decremented every 100 milliseconds
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
    			System.out.println("Error: when attempted to interrupt the ClientWatchdog when the thread is waiting, sleeping, or otherwise occupied");
	            System.out.println(IntEx.getMessage());
    			break; 
    		} 
    	} 
    }
    
}
