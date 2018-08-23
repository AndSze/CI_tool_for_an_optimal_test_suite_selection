package tcpServer;

import java.io.IOException;
import watchdog.Global_1h_Watchdog;
import watchdog.Global_24h_Watchdog;

public class TCPserver_Teardown {
	
	public TCPserver_Teardown() {
		
	}
	
	public void reinitalize_to_default(TCPserver tcp_server) throws IOException {
		
		if(tcp_server != null) {
			if(tcp_server.getServerSocket() != null) {
				if (tcp_server.getServerSocket().isBound()) {
					tcp_server.getServerSocket().close();
				}
				tcp_server.setServerSocket(null);
			}
			if(tcp_server.getServerThread() != null) {
				tcp_server.setServerThread(null);
			}
			tcp_server.set_ComputeEngineRunning(false);
			TCPserver.set_ServerRunning(false);
			if(TCPserver.get_1hWatchog_timestamp_table() != null) {
				TCPserver.set_1hWatchog_Allfalse(); 
			}
			if(TCPserver.get_24hWatchog_timestamp_table() != null) {
				TCPserver.set_24hWatchog_Allfalse();
			}
		}

		if(Global_1h_Watchdog.getInstance().getEnabled()) {
			Global_1h_Watchdog.getInstance().setEnabled(false);
			Global_1h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_1h_Watchdog.getInstance().getExpiration() * TCPserver.getWatchdogs_scale_factor());
			Global_1h_Watchdog.setM_instance(null);
		}
		if(Global_24h_Watchdog.getInstance().getEnabled()) {
			Global_24h_Watchdog.getInstance().setEnabled(false);
			Global_24h_Watchdog.getInstance().setTimeLeftBeforeExpiration(Global_24h_Watchdog.getInstance().getExpiration() * TCPserver.getWatchdogs_scale_factor() * TCPserver.getMeasurements_limit());
			Global_24h_Watchdog.setM_instance(null);
		}

		if (TCPserver.Server_Sensors_LIST.size() != 0) {
			for(int i = 0; i < TCPserver.Server_Sensors_LIST.size(); i++) {
				TCPserver.Server_Sensors_LIST.remove(i);
			}
			TCPserver.Server_Sensors_LIST.clear();
		}
		if (TCPserver.MeasurementHistory_LIST.size() != 0) {
			for(int i = 0; i < TCPserver.MeasurementHistory_LIST.size(); i++) {
				TCPserver.MeasurementHistory_LIST.remove(i);
			}
			TCPserver.MeasurementHistory_LIST.clear();
		}
		if (TCPserver.MeasurementData_LIST.size() != 0) {
			for(int i = 0; i < TCPserver.MeasurementData_LIST.size(); i++) {
				TCPserver.MeasurementData_LIST.remove(i);
			}
			TCPserver.MeasurementData_LIST.clear();
		}
		if (TCPserver.getProcessing_engine() != null) {
			TCPserver.getProcessing_engine().deleteAllFilesFromDirectiory(TCPserver.getSensorsPath());
		}
		
		TCPserver.setTCPserver_instance(null);

		System.out.println("[TCPserver] all attributes of the static TCPserver class are reinitialized to default values");
	}

}
