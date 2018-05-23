package messages;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class ServerMessage_SensorInfoUpdate extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected ServerMessage sensorMessage;
	protected Point2D.Float coordinates;
	protected String softwareImageID;
	protected SensorState sensorState;
	protected double _1h_Watchdog;
	protected double _24h_Watchdog;
	
	// ServerMessage_SensorInfoUpdate class constructor
	public ServerMessage_SensorInfoUpdate(int sensorID, Point2D.Float coordinates, String softwareImageID, SensorState sensorState, double _1h_Watchdog, double _24h_Watchdog) {
		super(sensorID);
		this.coordinates = coordinates;
		this.softwareImageID = softwareImageID;
		this.sensorState = sensorState;
		this._1h_Watchdog = _1h_Watchdog;
		this._24h_Watchdog = _24h_Watchdog;
		this.sensorMessage = ServerMessage.SENSOR_INFO_UPDATE;
	}

	public Point2D.Float getCoordinates() {
		return coordinates;
	}

	public String getSoftwareImageID() {
		return softwareImageID;
	}

	public SensorState getSensorState() {
		return sensorState;
	}
	
	public double get1h_Watchdog() {
		return _1h_Watchdog;
	}
	
	public double get24h_Watchdog() {
		return _24h_Watchdog;
	}

}