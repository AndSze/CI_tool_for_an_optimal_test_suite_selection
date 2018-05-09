package tcpServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import sensor.MeasurementData;
import sensor.SensorImpl;

public class ComputeEngine extends TCPserver implements TCPserver_interface, Runnable {
	
    private static int computeEnginesRunningID   = 0;
    private PrintStream outputStream = null;
    private InputStreamReader inputStream = null;
    private int timeout = 0;
	
	public ComputeEngine(Socket clientSocket) throws IOException  {
		super();
		inputStream = new InputStreamReader(clientSocket.getInputStream());
    	outputStream = new PrintStream(clientSocket.getOutputStream(), true);
    	// create lists for objects that are already saved in the server directory
    	
    	try {
			setSerializedObjectList(Server_Sensors_LIST, MeasurementData_LIST, MeasurementHistory_LIST);
        } catch (ClassNotFoundException CNFex) {
            System.out.println("Error: when new ComputeEngine failed due to class of a deserialized object cannot be found");
        	System.out.println(CNFex.getMessage());
        }
    	ComputeEngine.computeEnginesRunningID  += 1;
        System.out.println("[ECHO Compute engine] Multithreaded Server Service for processing Client Request no: "+ ComputeEngine.computeEnginesRunningID + " has been started");

	}

    public void run() {
      
    	//synchronized (Echo) {
    		
    		try {
    		BufferedReader bufferedReader = new BufferedReader(inputStream);
            String message = null;
            timeout = 0;
            
            while(timeout<10)
            {
    			if(bufferedReader.ready())
    			{
	            	long time = System.currentTimeMillis();
	            	message = bufferedReader.readLine();
	            	EchoResponse(outputStream, message, time);
	            	timeout = 0;
    			}
    			else
    			{
    				timeout = timeout+1;
    				processingDelay(1000);
    			}
    			//processingDelay(10);
            }  
        } catch (IOException IOex) {
        	System.out.println("Error: when attempted to read bufferedReaderinputStream on the client side");
        	IOex.printStackTrace();
        } finally {
        		closeOutStream();
        	try {
				closeInStream();
			} catch (IOException IOex) {
			    System.out.println("Error: when attempted to close InputStreamReader inputStream on the client side");
			    IOex.printStackTrace();
			}
          }
    	//}
    }
    

	public /*synchronized*/ void EchoResponse(PrintStream outputStream, String message, long time) {

		String server_message = null;
		
        System.out.println("message received from cliennnt: \n\t"+message);
        //processingDelay(1000);
        server_message =  Integer.toString(computeEnginesRunningID*Integer.parseInt(message));
        server_message = "Let's try ComputerEngine ID: " + computeEnginesRunningID+ " that resends: "+server_message;
        
        System.out.println("Send back the following message: "+server_message);
        
        outputStream.println(server_message);
        System.out.println("Request processed: " + time);
	}
	
	public void closeOutStream() {
		if (outputStream!=null) {
			outputStream.close();
		}
	}
	
	public void closeInStream() throws IOException {
		if (inputStream!=null) {
			inputStream.close();
		}
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
            	setNumberOfSensors(getNumberOfSensors() + 1);
            	
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
            }
            if(getNumberOfSensors() != 0)
            {
            	System.out.println("Copy existing " + getNumberOfSensors() + 
            			" Sensors stored in the directory to a list that store these events");
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
	public void saveSensorInfo(SensorImpl sensor){
		TCPserver.Server_Sensors_LIST.add(sensor);
		serialize(sensor, getSensorPath(sensor));
	}
	

	@Override
	public void saveMeasurementDataInfo(SensorImpl sensor, MeasurementData m_data) {
			TCPserver.MeasurementData_LIST.add(m_data);
			serialize(m_data, getMeasurementDataPath(sensor, m_data));
	}
	
	@Override
	public void saveMeasurementHistoryInfo(SensorImpl sensor, MeasurementData[] m_history) {
			TCPserver.MeasurementHistory_LIST.add(m_history);
			serialize(m_history, getMeasurementHistoryPath(sensor, m_history));
	}
	
	@Override
	public boolean compareMeasurementDataAgainstMeasurementHistory(SensorImpl sensor, MeasurementData[] m_history) throws ClassNotFoundException {
		
		boolean success = false;
		ArrayList<String> serialized_m_data_paths = new ArrayList<>();
		
		// get paths for all deserialize MeasurementData objects and save them to serialized_m_data_paths
		File sensor_path = null;
		sensor_path = new java.io.File(TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID());
		for (File file :  sensor_path.listFiles()) {
			if(file.toString().toString().substring(file.toString().length() - 16).equals("measurement_data")) {
				serialized_m_data_paths.add(file.toString());
			}
		}
		
		// deserialize MeasurementData object and save them to mes_data_to_compare
		ArrayList<MeasurementData> mes_data_to_compare = new ArrayList<>();
		for (String path :  serialized_m_data_paths) {
			mes_data_to_compare.add((MeasurementData) deserialize(path));
		}
		
		// deserialize array of  MeasurementData objects and save it to mes_hist_to_compare
		MeasurementData[] mes_hist_to_compare = new MeasurementData[24];
		mes_hist_to_compare = (MeasurementData[]) deserialize(getMeasurementHistoryPath(sensor, m_history));
		int i = 0;
		
		for(MeasurementData m_data : mes_data_to_compare) {
			if (m_data != mes_hist_to_compare[i]) {
				break;
			} else if (i==23) {
				success = true;
			}
			i++;
		}
		
		return success;
	}
	
	@Override
	public void deleteMeasurementDataInfo(SensorImpl sensor) {
		// delete 24 MeasurementData files after saving the date in the getMeasurementHistoryPath
		ArrayList<File> serialized_m_datas = new ArrayList<>();
		File sensor_path = null;
		sensor_path = new java.io.File(TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID());
		for (File file :  sensor_path.listFiles()) {
			if(file.toString().toString().substring(file.toString().length() - 16).equals("measurement_data")) {
				serialized_m_datas.add(file);
			}
		}
		
		if(serialized_m_datas.size() == 24) {
			for (File file :  serialized_m_datas) {
				file.delete();
			}
		} else {
			// error handling
		}
	}
	
	@Override
	public String getSensorPath(SensorImpl sensor){
		String sensor_path = null;
		String sensor_serialized_file_path = null;
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID();
		sensor_serialized_file_path = sensor_path + "\\" + "sensor_" + sensor.getSensorID() + ".sensor_info";
		return sensor_serialized_file_path;
	}
	
	@Override
	public String getMeasurementDataPath(SensorImpl sensor, MeasurementData m_data){
		String sensor_path = null;
		String measurementData_serialized_file_path = null;
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID();
		measurementData_serialized_file_path = sensor_path + "\\" + "measurement_" + m_data.getTimestamp() + ".measurement_data";
		return measurementData_serialized_file_path;
	}

	@Override
	public String getMeasurementHistoryPath(SensorImpl sensor, MeasurementData[] m_hist) {
		String sensor_path = null;
		String measurementHistory_serialized_file_path = null;
		String date_Timestamp = null;
		// remove time from the m_hist[0] element timestamp
		date_Timestamp = m_hist[0].getTimestamp().substring(0, m_hist[0].getTimestamp().length() - 9);;
		sensor_path = TCPserver.Sensors_PATH + "\\" + "sensor_" + sensor.getSensorID();
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
	public boolean serialize(Object obj, String path){
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(obj);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in " + path);
			return true;
		} catch (IOException i) {
			i.printStackTrace();
			return false;
		}
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
}
