package tcpServer;

import java.io.File;
import java.util.ArrayList;
import sensor.MeasurementData;
import sensor.MeasurementHistory;
import sensor.SensorImpl;

	/*
	 * INTERFACES ENABLE THE USER TO REFER TO INSTANCES OF THE CLASS THAT IMPLEMENTS THE INTERFACE
     * THROUGH TCPserver interface
     */ 

public interface TCPserver_interface {
	
	public void processClinetMessage();
	public void sendMessage();
	public void run();
	
	// Create containers where received Sensor, Measurement data and Measurement history info will be stored on the server side
	public void setSerializedObjectList(ArrayList<SensorImpl> sensors_list, ArrayList<MeasurementData> mes_data_list, ArrayList<MeasurementData[]> mes_hist_list) throws ClassNotFoundException;
	
    // Register Sensor on the server side (add to Sensor List) and save it in the specified path
    public void saveSensorInfo(SensorImpl sensor) throws ClassNotFoundException;
    
    // Register Measurement data on the server side (add to the Measurement Data List) and save it in the specified path
    public void saveMeasurementDataInfo(SensorImpl sensor, MeasurementData m_data);
    
    // Register Measurement History on the server side (add to the Measurement History List) and save it in the specified path
    public void saveMeasurementHistoryInfo(SensorImpl sensor, MeasurementData[] m_hist);
    
    // Compare if the 24 previously received and serialized Measurement Data items are the same as the Measurement History received from the Sensor
    public boolean compareMeasurementDataAgainstMeasurementHistory(SensorImpl sensor, MeasurementData[] m_history) throws ClassNotFoundException;
    
    // if compareMeasurementDataAgainstMeasurementHistory returns true, delete 24 serialized Measurement Data items from MeasurementData_PATH after saving that data in MeasurementHistory_PATH
    public void deleteMeasurementDataInfo(SensorImpl sensor);
    
	// List all object saved in the specified path
    public ArrayList<String> getObjectsPath(File folder); // used for getting to any List
    public String getSensorPath(SensorImpl sensor);
    public String getMeasurementHistoryPath(SensorImpl sensor, MeasurementData[] m_hist);
    public String getMeasurementDataPath(SensorImpl sensor, MeasurementData m_data);
    
	// Listing all object saved in the specified path
	public ArrayList<String> getObjectList(File folder);
    
    // Save data to a serialized file
    boolean serialize(Object obj, String path);
    
    // Retrieve data from a serialized file
    Object deserialize(String path) throws ClassNotFoundException;
    
    
	
}
