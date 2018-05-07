package sensor;

public class MeasurementData {

	// class attributes
	protected float pm25;
	protected float pm10;
	protected int humidity;
	protected int temperature;
	protected int pressure;
	
	// MeasurementData class constructor
	public MeasurementData(float pm25, float pm10, int humidity, int temperature, int pressure) {
		super();
		this.pm25 = pm25;
		this.pm10 = pm10;
		this.humidity = humidity;
		this.temperature = temperature;
		this.pressure = pressure;
	}

	public float getPm25() {
		return pm25;
	}

	public float getPm10() {
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
