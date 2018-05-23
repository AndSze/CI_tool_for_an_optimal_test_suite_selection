package messages;

import java.io.Serializable;

public class ServerMessage_ACK extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected ServerMessage sensorMessage;
	protected double _1h_Watchdog;
	protected double _24h_Watchdog;
	
	// ServerMessage_ACK class constructor
	public ServerMessage_ACK(int sensorID, double _1h_Watchdog, double _24h_Watchdog) {
		super(sensorID);
		this._1h_Watchdog = _1h_Watchdog;
		this._24h_Watchdog = _24h_Watchdog;
		this.sensorMessage = ServerMessage.ACK;
	}

	public double get1h_Watchdog() {
		return _1h_Watchdog;
	}
	
	public double get24h_Watchdog() {
		return _24h_Watchdog;
	}
	
}
