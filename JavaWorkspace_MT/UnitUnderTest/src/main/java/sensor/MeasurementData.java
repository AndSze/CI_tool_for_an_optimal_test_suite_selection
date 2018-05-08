package sensor;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MeasurementData {

	// class attributes
	protected float pm25;
	protected float pm10;
	protected int humidity;
	protected int temperature;
	protected int pressure;
	protected String timestamp;

	// MeasurementData class constructor
	public MeasurementData() {
		super();
		this.timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
	}

	public void setPm25(float pm25) {
		this.pm25 = pm25;
	}

	public void setPm10(float pm10) {
		this.pm10 = pm10;
	}

	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	public void setPressure(int pressure) {
		this.pressure = pressure;
	}

	public String getTimestamp() {
		return timestamp;
	}
}
