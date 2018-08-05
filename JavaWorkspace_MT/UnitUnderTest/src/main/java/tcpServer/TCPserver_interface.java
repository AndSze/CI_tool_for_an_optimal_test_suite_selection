package tcpServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import sensor.MeasurementData;
import sensor.SensorImpl;

	/*
	 * INTERFACES ENABLE THE USER TO REFER TO INSTANCES OF THE CLASS THAT IMPLEMENTS THE INTERFACE
     * THROUGH TCPserver interface
     */ 

public interface TCPserver_interface {
	
	public void processClinetMessage();
	public void sendMessage();
	
	// Create containers where received Sensor, Measurement data and Measurement history info will be stored on the server side
	public void setSerializedObjectList(ArrayList<SensorImpl> sensors_list, ArrayList<MeasurementData> mes_data_list, ArrayList<MeasurementData[]> mes_hist_list) throws ClassNotFoundException;
	
    // Since sensor are defined prior to execution, this function only serializes the sensor instances in the specified path
	// to register Sensor on the server side (add to Sensor List) the updateServerSensorList function should be called
    public void saveSensorInfo(SensorImpl sensor, String action) throws IOException, FileNotFoundException;
    
    // this function is called to update the Server Sensor List every time the new or updated sensor info is received 
    public ArrayList<SensorImpl> updateServerSensorList(SensorImpl sensor);
    public SensorImpl searchInServerSensorList(int sensor_ID);
    		
    // Register Measurement data on the server side (add to the Measurement Data List) and serialize it in the specified path
    public void saveMeasurementDataInfo(SensorImpl sensor, MeasurementData m_data) throws IOException, FileNotFoundException;
    
    // Register Measurement History on the server side (add to the Measurement History List) and serialize it in the specified path
    public void saveMeasurementHistoryInfo(SensorImpl sensor, MeasurementData[] m_hist)throws IOException, FileNotFoundException;
    
    // Compare if the 24 previously received and serialized Measurement Data items are the same as the Measurement History received from the Sensor
    //public boolean compareMeasurementDataAgainstMeasurementHistory(SensorImpl sensor, MeasurementData[] m_history) throws ClassNotFoundException;
    
    // if compareMeasurementDataAgainstMeasurementHistory returns true, delete 24 serialized Measurement Data items from MeasurementData_PATH after saving that data in MeasurementHistory_PATH
    public void deleteMeasurementDataInfo(SensorImpl sensor);
    public void deleteAllFilesFromDirectiory(String path);
    
	// List all object saved in the specified path
    public ArrayList<String> getObjectsPath(File folder); // used for getting to any List
    public String getSensorPath(SensorImpl sensor, String info_type);
    public String getMeasurementHistoryPath(SensorImpl sensor, MeasurementData[] m_hist);
    public String getMeasurementDataPath(SensorImpl sensor, MeasurementData m_data);
    
	// Listing all object saved in the specified path
	public ArrayList<String> getObjectList(File folder);
    
    // Save data to a serialized file
    boolean serialize(Object obj, String path) throws IOException;
    
    // Retrieve data from a serialized file
    public <T> T  deserialize(String path, Class<T> type) throws ClassNotFoundException;
    
	
}
