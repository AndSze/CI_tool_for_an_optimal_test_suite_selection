package messages;

import java.io.Serializable;
import sensor.MeasurementData;

public class ClientMessage_MeasurementData extends Message_Interface implements Serializable{

	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected MeasurementData measurementData;
	protected SensorMessage sensorMessage;
	
	// ClientMessage_MeasurementData class constructor
	public ClientMessage_MeasurementData(int sensorID, MeasurementData measurementData) {
		super();
		this.sensorID = sensorID;
		this.measurementData = measurementData;
		this.sensorMessage = SensorMessage.DATA;
	}

}
