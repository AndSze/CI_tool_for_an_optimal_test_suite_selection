package tcpServer;

import java.rmi.RemoteException;

public interface TCPserver_interface {
	
	public void processClinetMessage();
	public void sendMessage();
	public void run();
	public boolean serialize(Object obj, String path) throws RemoteException, ClassNotFoundException;
	public Object deserialize(String path) throws RemoteException, ClassNotFoundException;
	
}
