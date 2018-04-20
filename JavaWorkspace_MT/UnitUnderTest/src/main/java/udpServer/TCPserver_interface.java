package udpServer;


public interface TCPserver_interface {
	
	public void listenForIncomingMessages();
	public void processClinetMessage();
	public void sendMessage();

}
