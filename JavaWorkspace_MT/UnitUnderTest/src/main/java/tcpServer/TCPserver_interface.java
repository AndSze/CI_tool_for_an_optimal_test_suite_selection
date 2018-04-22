package tcpServer;

import java.io.IOException;
import java.net.ServerSocket;

public interface TCPserver_interface {
	
	public void processClinetMessage();
	public void sendMessage();
	public void run();

}
