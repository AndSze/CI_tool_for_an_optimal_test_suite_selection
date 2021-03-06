package sensor;

import java.awt.geom.Point2D;
import java.io.Serializable;

import messages.SensorState;

public class SensorImpl extends MeasurementData implements Serializable {
	
	// Explicit declaration of serialVersionUID, hence InvalidClassExceptions will never be thrown during deserialization
	private static final long serialVersionUID = 1L;
		
	// class attributes
	protected int sensorID;
	protected Point2D.Float coordinates = null;
	protected String softwareImageID = null;
	protected SensorState sensorState = SensorState.PRE_OPERATIONAL;
	// sensor_m_history_array_size is used for determining after how many measurement datas, the measurement history request is sent - variable is set to a value received from the server in ServerMessage_SensorInfoUpdate
	private int sensor_m_history_array_size = 0;
	protected MeasurementData[] sensor_m_history;
	protected int numberOfMeasurements = 0;
	// watchdog scale factor is used for scaling the watchdog expiration times - variable is set to a value received from the server in ServerMessage_SensorInfoUpdate
	protected double sensor_watchdog_scale_factor = 1.0;
	
	// Default SensorImpl class constructor - for the client side
	public SensorImpl(int sensorID) {
		super();
		this.sensorID = sensorID;
	}

	// Overloaded SensorImpl class constructor - for the server side
	public SensorImpl(int sensorID, Point2D.Float coordinates, String softwareImageID, int sensor_m_history_array_size) {
		super();
		this.sensorID = sensorID;
		this.coordinates = coordinates;
		this.softwareImageID = softwareImageID;
		this.sensor_m_history_array_size = sensor_m_history_array_size;
		sensor_m_history =  new MeasurementData[sensor_m_history_array_size];
	}
	
	public void resetSensor() {
		this.sensor_m_history = new MeasurementData[this.sensor_m_history_array_size];
		setNumberOfMeasurements(0);
		setSensorState(SensorState.PRE_OPERATIONAL);
	}
	
	public boolean addMeasurement(double pm25, double pm10, int humidity, int temperature, int pressure) {
		boolean success = false;
		if (getSensorState() == SensorState.OPERATIONAL) {
			try {
				int temp_numberOfMeasurements = getNumberOfMeasurements();
				//System.out.println("[SensorImpl] Sensor: \t" + sensorID + " has the following number of measurements:\t" + temp_numberOfMeasurements);
				
				// overwrite last measurement in sensor_m_history to prevent inserting a measurement to the out-of-bound array index
				if (temp_numberOfMeasurements == sensor_m_history_array_size) {
					temp_numberOfMeasurements = temp_numberOfMeasurements - 1;
				}
				// overwrite the MeasurementData class instance to get current timestamp
				this.sensor_m_history[temp_numberOfMeasurements] = new MeasurementData();
				
				this.sensor_m_history[temp_numberOfMeasurements].setPm25(pm25);
				this.sensor_m_history[temp_numberOfMeasurements].setPm10(pm10);
				this.sensor_m_history[temp_numberOfMeasurements].setHumidity(humidity);
				this.sensor_m_history[temp_numberOfMeasurements].setTemperature(temperature);
				this.sensor_m_history[temp_numberOfMeasurements].setPressure(pressure);
				setNumberOfMeasurements(temp_numberOfMeasurements + 1);
				
				// Measurements is successful
				success = true;
			} catch (ArrayIndexOutOfBoundsException AIOOBex) {
				System.out.println("Error: Sensor Measurement History has exceeded it allowed size, Sensor goes to the DEAD state");
				setSensorState(SensorState.DEAD);
				AIOOBex.printStackTrace();
			}
		} else {
			int temp_numberOfMeasurements = getNumberOfMeasurements();
			
			// overwrite the MeasurementData class instance to get current timestamp
			this.sensor_m_history[temp_numberOfMeasurements] = new MeasurementData();
			
			setNumberOfMeasurements(temp_numberOfMeasurements + 1);
			
			//cannot measure, because the sensor is not in the OPERETIONAL state
			success = false;
		}
		return success;
	}
	
	public MeasurementData readLastMeasurementData() {
		MeasurementData returnedMeasurementData = null;
		int temp_numberOfMeasurements = getNumberOfMeasurements() - 1;
		if(getNumberOfMeasurements()>0) {
			returnedMeasurementData =  sensor_m_history[temp_numberOfMeasurements];
		}
		return returnedMeasurementData;
	}
	
	public MeasurementData[] readMeasurementHistory() {
		return sensor_m_history;
	}
	
	public void setMeasurementHistory(MeasurementData[] sensor_m_history) {
		this.sensor_m_history = sensor_m_history;
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
	
	public int getNumberOfMeasurements() {
		return numberOfMeasurements;
	}

	public void setNumberOfMeasurements(int numberOfMeasurements) {
		this.numberOfMeasurements = numberOfMeasurements;
	}

	public double getLocal_watchdog_scale_factor() {
		return sensor_watchdog_scale_factor;
	}

	public void setSensor_watchdog_scale_factor(double local_watchdog_scale_factor) {
		this.sensor_watchdog_scale_factor = local_watchdog_scale_factor;
	}
	
	public int getSensor_m_history_array_size() {
		return sensor_m_history_array_size;
	}


}
