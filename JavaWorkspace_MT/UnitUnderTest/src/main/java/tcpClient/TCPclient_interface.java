package tcpClient;

import java.rmi.RemoteException;

public interface TCPclient_interface {
	
	
	public void hadleIncomingMessage();
	public void sendMessage();
	public void triggerComputeEngine();
	public boolean serialize(Object obj, String path) throws RemoteException, ClassNotFoundException;
	public Object deserialize(String path) throws RemoteException, ClassNotFoundException;
	

}
