package sensor;

import java.awt.geom.Point2D;
import messages.SensorState;

public class SensorImpl {
	
	// class attributes
	protected int sensorID;
	protected Point2D.Float coordinates;
	protected String softwareImageID;
	protected SensorState sensorState;
	
	// SensorImpl class constructor
	public SensorImpl(int sensorID, Point2D.Float coordinates, String softwareImageID, SensorState sensorState) {
		super();
		this.sensorID = sensorID;
		this.coordinates = coordinates;
		this.softwareImageID = softwareImageID;
		this.sensorState = sensorState;
	}
	
	public int getSensorID() {
		return sensorID;
	}
	public void setSensorID(int sensorID) {
		this.sensorID = sensorID;
	}
	public Point2D.Float getCoordinates() {
		return coordinates;
	}
	public void setCoordinates(Point2D.Float coordinates) {
		this.coordinates = coordinates;
	}
	public String getSoftwareImageID() {
		return softwareImageID;
	}
	public void setSoftwareImageID(String softwareImageID) {
		this.softwareImageID = softwareImageID;
	}
	public SensorState getSensorState() {
		return sensorState;
	}
	public void setSensorState(SensorState sensorState) {
		this.sensorState = sensorState;
	}

}
