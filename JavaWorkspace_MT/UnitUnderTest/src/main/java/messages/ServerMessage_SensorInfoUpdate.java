package messages;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class ServerMessage_SensorInfoUpdate extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected int sensorID_old;
	protected ServerMessage sensorMessage;
	protected int sensorID_new;
	protected Point2D.Float coordinates;
	protected String softwareImageID;
	protected SensorState sensorState;
	
	// ServerMessage_SensorInfoUpdate class constructor
	public ServerMessage_SensorInfoUpdate(int sensorID_old, ServerMessage sensorMessage, int sensorID_new,
			Point2D.Float coordinates, String softwareImageID, SensorState sensorState) {
		super();
		this.sensorID_old = sensorID_old;
		this.sensorID_new = sensorID_new;
		this.coordinates = coordinates;
		this.softwareImageID = softwareImageID;
		this.sensorState = sensorState;
		this.sensorMessage = ServerMessage.SENSOR_INFO_UPDATE;
	}

}