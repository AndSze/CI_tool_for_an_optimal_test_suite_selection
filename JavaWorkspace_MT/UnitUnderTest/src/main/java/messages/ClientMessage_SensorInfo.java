package messages;

import java.io.Serializable;
import sensor.SensorImpl;

public class ClientMessage_SensorInfo extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected SensorImpl sensor;
	protected SensorMessage sensorMessage;
	
	// ClientMessage_SensorInfo class constructor
	public ClientMessage_SensorInfo(SensorImpl sensor) {
		super();
		this.sensorID = sensor.getSensorID();
		this.sensor = sensor;
		this.sensorMessage = SensorMessage.SENSOR_INFO;
	}
}
