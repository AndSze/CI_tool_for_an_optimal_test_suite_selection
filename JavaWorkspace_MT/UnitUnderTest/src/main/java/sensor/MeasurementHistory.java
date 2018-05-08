package sensor;

public class MeasurementHistory {

	// class attributes
		private final int hisotry_array_size = 24;
		protected float[] pm25_history = null;
		protected float[] pm10_history = null;
		protected float[] humidity_history= null;
		protected float[] temperature_history = null;
		protected float[] pressure_history = null;
		protected static int numberOfElements = 0;
		
	// MesurementHistory class default constructor
	public MeasurementHistory() {
		super();
		this.pm25_history = new float[hisotry_array_size];
		this.pm10_history = new float[hisotry_array_size];
		this.humidity_history = new float[hisotry_array_size];
		this.temperature_history = new float[hisotry_array_size];
		this.pressure_history = new float[hisotry_array_size];
	}
	
	public void addElement(Float element, Measurement_IDs id) {
		if (numberOfElements < 24) {
			switch(id.getMeasurement_IDs()) {
				case 0:	pm25_history[numberOfElements] = element;
						break;
				case 1:	pm10_history[numberOfElements] = element;
						break;
				case 2:	humidity_history[numberOfElements] = element;
						break;
				case 3:	temperature_history[numberOfElements] = element;
						break;
				case 4:	pressure_history[numberOfElements] = element;
						break;
				default: break;
			}
		} else {
			switch(id.getMeasurement_IDs()) {
			case 0:	pm25_history[numberOfElements-1] = element;
					break;
			case 1:	pm10_history[numberOfElements-1] = element;
					break;
			case 2:	humidity_history[numberOfElements-1] = element;
					break;
			case 3:	temperature_history[numberOfElements-1] = element;
					break;
			case 4:	pressure_history[numberOfElements-1] = element;
					break;
			default: break;
			}
		}
	}
	
	public float readLastElement(Measurement_IDs id) {
		float returned_element = 0;
		if (numberOfElements < 24) {
			switch(id.getMeasurement_IDs()) {
				case 0:	returned_element = pm25_history[numberOfElements];
				case 1:	returned_element = pm10_history[numberOfElements];
				case 2:	returned_element = humidity_history[numberOfElements];
				case 3:	returned_element = temperature_history[numberOfElements];
				case 4:	returned_element = pressure_history[numberOfElements];
				default: break;
			}
		} else {
			switch(id.getMeasurement_IDs()) {
				case 0:	returned_element = pm25_history[numberOfElements-1];
				case 1:	returned_element = pm10_history[numberOfElements-1];
				case 2:	returned_element = humidity_history[numberOfElements-1];
				case 3:	returned_element = temperature_history[numberOfElements-1];
				case 4:	returned_element = pressure_history[numberOfElements-1];
				default: break;
		}
		}
		return returned_element;
	}
	
	public float[] readWholeArray(Measurement_IDs id) {
		float[] returned_array = new float[hisotry_array_size];
		switch(id.getMeasurement_IDs()) {
			case 0:	returned_array = pm25_history;
			case 1:	returned_array = pm25_history;
			case 2:	returned_array = humidity_history;
			case 3:	returned_array = temperature_history;
			case 4:	returned_array = pressure_history;
			default: break;
		}
		return returned_array;
	}
	
}
