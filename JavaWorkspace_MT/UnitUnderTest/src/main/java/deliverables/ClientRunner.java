package deliverables;

import java.io.IOException;
import java.util.ArrayList;

public class ClientRunner {
	
	private static ArrayList<UUT_TCPclient> clientInstancesList = new ArrayList<UUT_TCPclient>();
	private final static int number_of_sensors = 6;
	
	public ClientRunner(int sensor_ID, int port) throws IOException {
		clientInstancesList.add(new UUT_TCPclient(sensor_ID, port));
	}
	/*
	public static void main(String []args) {
		for (int i = 1; i<=number_of_sensors; i++ ) {
			try {
				new ClientRunner(i, 9876);
			} catch (IOException e) {
				System.out.println("Error: Instance for the TCP client for sensor ID: "+i+" cannot be created");
				e.printStackTrace();
			}
		}
		
		for (UUT_TCPclient clientInstance : clientInstancesList ) {
			try {
				clientInstance.start(clientInstance);
				Thread.sleep(100);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}*/


}
