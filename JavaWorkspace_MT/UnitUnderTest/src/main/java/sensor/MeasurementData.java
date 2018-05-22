package sensor;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MeasurementData {

	// class attributes
	protected double pm25;
	protected double pm10;
	protected int humidity;
	protected int temperature;
	protected int pressure;
	protected String timestamp;

	// MeasurementData class constructor
	public MeasurementData() {
		super();
		this.timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
	}

	public void setPm25(double pm25) {
		this.pm25 = pm25;
	}

	public void setPm10(double pm10) {
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

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getTimestamp() {
		return timestamp;
	}
	
	public double getPm25() {
		return pm25;
	}

	public double getPm10() {
		return pm10;
	}
	
	public int getHumidity() {
		return humidity;
	}
	
	public int getTemperature() {
		return temperature;
	}
	
	public int getPressure() {
		return pressure;
	}
	
}
