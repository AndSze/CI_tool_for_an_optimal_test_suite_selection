package tcpServer;

import static org.junit.Assert.*;

import org.junit.Test;

import messages.Message_Interface;

public class ComputeEngine_RunnableTest {

	

	
	@Test
	public void test() {
		fail("Not yet implemented");
		
		@SuppressWarnings("serial")
		class ClientMessage_NotSerializable extends Message_Interface{
			// ClientMessage_MeasurementHistory class constructor
			public ClientMessage_NotSerializable(int sensorID) {
				super(sensorID);
			}
		}
		
		//ClientMessage_NotSerializable invalid_message = new ClientMessage_NotSerializable(sensor_ID_1);
		
		//mockComputeEngine_Runnable.getOutputStream().writeObject(invalid_message);
		//Thread.sleep(10);
	}

}
