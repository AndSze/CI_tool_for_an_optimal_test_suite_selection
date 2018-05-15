package messages;

import java.io.Serializable;

import sensor.MeasurementData;


public class ClientMessage_MeasurementHistory extends Message_Interface implements Serializable{
	
	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
	
	// class attributes
	protected SensorMessage sensorMessage;
	protected MeasurementData[] mes_history = new MeasurementData[24];
	
	// ClientMessage_MeasurementHistory class constructor
	public ClientMessage_MeasurementHistory(int sensorID, MeasurementData[] mes_history) {
		super();
		this.sensorID = sensorID;
		this.sensorMessage = SensorMessage.HISTORY;
		this.mes_history = mes_history;
	}

}
