package tcpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import sensor.MeasurementData;
import sensor.SensorImpl;

public class ComputeEngine_Processing extends TCPserver implements TCPserver_interface {
	
	private int number_of_sensors = 0;

	public ComputeEngine_Processing () throws IOException, ClassNotFoundException  {
    	// create lists for objects that are already saved in the server directory
    	setSerializedObjectList(Server_Sensors_LIST, MeasurementData_LIST, MeasurementHistory_LIST);
	}

	
	static void processingDelay(int msec) {
	    try {
	        Thread.sleep(msec);
	    } catch (InterruptedException ex) {
	        
	    }
    }


	public void processClinetMessage() {
		// TODO Auto-generated method stub
		
	}

	public void sendMessage() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void setSerializedObjectList(ArrayList<SensorImpl> Server_Sensors_LIST, ArrayList<MeasurementData> mes_data_list, ArrayList<MeasurementData[]> mes_hist_list) throws ClassNotFoundException {
		// Create a directory where Single Events will be saved on the server side
		boolean success = (new File(TCPserver.Sensors_PATH)).mkdirs();
		if (!success) 
		{
			ArrayList<String> MySensors = getObjectList(new File(TCPserver.Sensors_PATH));
			
            for (String sensor_path : MySensors)
            {
            	ArrayList<String> MySensorsData = getObjectList(new File(sensor_path));
            	
            	
            	for (String file : MySensorsData){
            		String sensor_data_path = TCPserver.Sensors_PATH + "\\" + file;
            		if ((file.substring(file.toString().length() - 11)).equals("sensor_info")){
            			SensorImpl new_sensor = (SensorImpl) deserialize(sensor_data_path);
            			Server_Sensors_LIST.add(new_sensor);
            		}
            		else if ((file.substring(file.toString().length() - 16)).equals("measurement_data")){
            			MeasurementData new_mes_data = (MeasurementData) deserialize(sensor_data_path);
            			mes_data_list.add(new_mes_data);
            		}
            		else if ((file.substring(file.toString().length() - 19)).equals("measurement_history")){
            			MeasurementData[] new_mes_hist = (MeasurementData[]) deserialize(sensor_data_path);
            			mes_hist_list.add(new_mes_hist);
            		}
            	}
            	setNumber_of_sensors(getNumber_of_sensors() + 1);
            }
            if(getNumber_of_sensors() != 0)
            {
            	System.out.println("Copy existing " + getNumber_of_sensors() + 
            			" Sensors stored in the directory to a list that store these sensors");
            }
            else
            {
            	System.out.println("There are no Sensors in the directory. The initialized Sensors List is empty");
            }
		}
		else
		{
			System.out.println("Creating a folder to store Sensors Data on the client side");
		}
	}

	@Override
	public void saveSensorInfo(SensorImpl sensor, String action) throws IOException{
		//dTCPserver.Server_Sensors_LIST.add(sensor);
		String temp_sensor_path = getSensorPath(sensor, action);
		serialize(sensor, temp_sensor_path);
	}
	

	@Override
	public void saveMeasurementDataInfo(SensorImpl sensor, MeasurementData m_data) throws IOException{
		TCPserver.MeasurementData_LIST.add(m_data);
		serialize(m_data, getMeasurementDataPath(sensor, m_data));
	}
	
	@Override
	public void saveMeasurementHistoryInfo(SensorImpl sensor, MeasurementData[] m_history) throws IOException{
		TCPserver.MeasurementHistory_LIST.add(m_history);
		serialize(m_history, getMeasurementHistoryPath(sensor, m_history));
	}
	/*
	@Override
	public boolean compareMeasurementDataAgainstMeasurementHistory(SensorImpl sensor, MeasurementData[] m_history) throws ClassNotFoundException {
		
		boolean success = false;
		ArrayList<String> serialized_m_data_paths = new ArrayList<>();
		
		// get paths for all deserialize MeasurementData objects and save them to serialized_m_data_paths
		File sensor_path = null;
		sensor_path = new java.io.File(TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID());
		for (File file :  sensor_path.listFiles()) {
			System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mesurement datas: "+ file.getName());
			if(file.toString().toString().substring(file.toString().length() - 16).equals("measurement_data")) {
				serialized_m_data_paths.add(file.toString());
			}
		}
		
		// deserialize MeasurementData object and save them to mes_data_to_compare
		ArrayList<MeasurementData> mes_data_saved_in_the_directory = new ArrayList<>();
		for (String path :  serialized_m_data_paths) {
			mes_data_saved_in_the_directory.add((MeasurementData) deserialize(path));
		}
		
		// deserialize array of  MeasurementData objects and save it to mes_hist_to_compare
		MeasurementData[] mes_hist_received = new MeasurementData[3];
		mes_hist_received = (MeasurementData[]) deserialize(getMeasurementHistoryPath(sensor, m_history));
		int i = 0;
		
		for(MeasurementData m_data : mes_data_saved_in_the_directory) {
			if (m_data.equals(mes_hist_received[i])) {
				/*
				System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mes_data_saved_in_the_directory Pm10 "+ m_data.getPm10());
				System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mes_data_saved_in_the_directory TimeStamp "+ m_data.getTimestamp());
				
				System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mes_hist_received[i] Pm10 "+ mes_hist_received[i].getPm10());
				System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mes_hist_received[i] TimeStamp "+ mes_hist_received[i].getTimestamp());
				
				System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mes_data_saved_in_the_directory equals mes_hist_received[i]");
			}
			else {
				System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] mes_data_saved_in_the_directory does not equal mes_hist_received[i]");
				break;
			}
			if (i==2) {
				success = true;
			}
			i++;
		}
		
		return success;
	}*/
	
	@Override
	public void deleteMeasurementDataInfo(SensorImpl sensor) {
		// delete 24 MeasurementData files after saving the date in the getMeasurementHistoryPath
		ArrayList<File> serialized_m_datas = new ArrayList<>();
		File sensor_path = null;
		sensor_path = new java.io.File(TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID()+ "\\" + "measurement_Datas");
		for (File file :  sensor_path.listFiles()) {
			//System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] substring(file.toString().length() - 16): " + file.toString().toString().substring(file.toString().length() - 16));
			if(file.toString().toString().substring(file.toString().length() - 16).equals("measurement_data")) {
				serialized_m_datas.add(file);
			}
		}
		
		if(serialized_m_datas.size() == TCPserver.getMeasurements_limit()) {
			for (File file :  serialized_m_datas) {
				//System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] the following measurement data: " + file.getName() + " is being deleted");
				file.delete();
			}
		} else {
			// error handling
		}
	}
	
	@Override
	public void deleteAllFilesFromDirectiory(String path) {
		String new_path = null;
		File file_path = null;
		File files_to_be_deleted_path = null;
		file_path = new java.io.File(path);
		String[]entries_1 = file_path.list();
		for (int i = 0; i< entries_1.length; i++) {
			new_path = path + "\\" + entries_1[i];
			file_path = new java.io.File(new_path);
			String[]entries_2 = file_path.list();
			for (int j = 0; j< entries_2.length; j++) {
				new_path = path + "\\" + entries_1[i] + "\\" + entries_2[j];
				files_to_be_deleted_path = new java.io.File(new_path);
				for (File file :  files_to_be_deleted_path.listFiles()) {
					//System.out.println("[Compute engine Processing] There is the following file in the directiory to be deleted: " + new_path + "\\" + entries_1[i] + "\\" + entries_2[j]);
					//System.out.println("[Compute engine Processing] the following file: " + file.getName() + " is being deleted");
					file.delete();
				}
				files_to_be_deleted_path.delete();
			}
			file_path.delete();
		}
		
	}
	
	@Override
	public String getSensorPath(SensorImpl sensor, String info_type){
		String sensor_path = null;
		String sensor_serialized_file_path = null;
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID();
		boolean success = (new File(sensor_path)).mkdirs();
		if(success) {
			System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] New folder for a sensor instance created");
		}
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID() + "\\" + "sensor_Infos";
		success = (new File(sensor_path)).mkdirs();
		if(success) {
			System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] New folder for a sensor info created");
		}
		String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime());
		sensor_serialized_file_path = sensor_path + "\\" + "sensor_" + sensor.getSensorID() + "_" + timestamp + "_" + info_type + ".sensor_info";
		return sensor_serialized_file_path;
	}
	
	@Override
	public String getMeasurementDataPath(SensorImpl sensor, MeasurementData m_data){
		String sensor_path = null;
		String measurementData_serialized_file_path = null;
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID() + "\\" + "measurement_Datas";
		boolean success = (new File(sensor_path)).mkdirs();
		if(success) {
			System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] New folder for MeasurementData created");
		}
		measurementData_serialized_file_path = sensor_path + "\\" + "measurement_" + m_data.getTimestamp() + ".measurement_data";
		return measurementData_serialized_file_path;
	}

	@Override
	public String getMeasurementHistoryPath(SensorImpl sensor, MeasurementData[] m_hist) {
		String sensor_path = null;
		String measurementHistory_serialized_file_path = null;
		String date_Timestamp = null;
		// remove time from the m_hist[0] element timestamp
		date_Timestamp = m_hist[0].getTimestamp().substring(0, m_hist[0].getTimestamp().length() - 3);;
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID() + "\\" + "measurement_Histories";
		boolean success = (new File(sensor_path)).mkdirs();
		if(success) {
			System.out.println("[Compute engine Processing " +sensor.getSensorID()+"] New folder for MeasurementHistory created");
		}
		measurementHistory_serialized_file_path = sensor_path + "\\" + "measurement_" + date_Timestamp + ".measurement_history";
		return measurementHistory_serialized_file_path;
	}
	
	@Override
	public ArrayList<String> getObjectList(File folder){
		File[] filesList = folder.listFiles();
		ArrayList<String> Files = new ArrayList<>();
		
		if (filesList != null)
		{
			for (File file : filesList)
			{
				if (file.isFile())
				{
					Files.add(file.getName());
				}
			}
		}
		return Files;
	}
	
	@Override
	public ArrayList<String> getObjectsPath(File folder){
		File[] filesList = folder.listFiles();
		ArrayList<String> Files = new ArrayList<>();
		
		if (filesList != null)
		{
			for (File file : filesList)
			{
				if (file.isFile())
				{
					Files.add(file.getName());
				}
			}
		}
		return Files;
	}
	
	@Override
	public boolean serialize(Object obj, String path) throws IOException {
		FileOutputStream fileOut = new FileOutputStream(path);
		ObjectOutputStream out = new ObjectOutputStream(fileOut);
		out.writeObject(obj);
		out.close();
		fileOut.close();
		System.out.println("Serialized data is saved in " + path);
		return true;
	}

	@Override
	public Object deserialize(String path) throws ClassNotFoundException {
		Object obj = null;
		try {
			FileInputStream fileIn = new FileInputStream(path);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			obj = in.readObject();
			in.close();
			fileIn.close();
			//System.out.println("Serialized data is retrieved from " + path);
			return obj;
		} catch (IOException i) {
			i.printStackTrace();
			return obj;
		}
	}
	
	@Override
	public synchronized ArrayList<SensorImpl> updateServerSensorList(SensorImpl sensor){
		int itemIndex = 0;
		if (Server_Sensors_LIST.size() == 0) {
			Server_Sensors_LIST.add(sensor);
		}
		else {
			for (SensorImpl s : Server_Sensors_LIST) {
				if (s.getSensorID() == sensor.getSensorID()) {
					Server_Sensors_LIST.set(itemIndex, sensor);
					break;
				} 
				else {
					itemIndex++; 
				}
			}
			if(itemIndex == (Server_Sensors_LIST.size())) {
				Server_Sensors_LIST.add(sensor);
			}
		}
		return Server_Sensors_LIST;
		
	}
	
	@Override
	public synchronized SensorImpl searchInServerSensorList(int sensor_ID){
		SensorImpl temp_sens = null;
		for (SensorImpl sens : Server_Sensors_LIST) {
			//System.out.println("Sensors stored in the sensors list on the server side, sensor ID: " + sens.getSensorID());
			if( sens.getSensorID() == sensor_ID) {
				temp_sens = sens;
				break;
			}
		}
		return temp_sens;
	}

	
	public int getNumber_of_sensors() {
		return number_of_sensors;
	}


	public void setNumber_of_sensors(int number_of_sensors) {
		this.number_of_sensors = number_of_sensors;
	}

}
