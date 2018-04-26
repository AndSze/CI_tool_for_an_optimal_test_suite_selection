package deliverables;

import tcpServer.TCPserver;

public class UUT_TCPserver {
	
    //create the port number
    private int port = 0;
    private TCPserver INSTANCE;
    
    UUT_TCPserver(int port) {
    	super();
    	this.port = port;
    	this.INSTANCE = TCPserver.getInstance();
    }
	
	public static void main(String []args) throws InterruptedException{
		
		UUT_TCPserver uut_TCPserver1 = new UUT_TCPserver(9876);
		runTheServer(uut_TCPserver1.getINSTANCE(), uut_TCPserver1.getPort());
		
		Thread.sleep(5000);

		closeTheServer(uut_TCPserver1.getINSTANCE(), 9876);

	}

	public static void runTheServer(TCPserver INSTANCE, int port)
	{
		INSTANCE.initServer(port);
	}
	
	public static void closeTheServer(TCPserver INSTANCE, int port)
	{
		INSTANCE.closeServer(port);;
	}
	
	
	public int getPort() {
		return this.port;
	}

	public TCPserver getINSTANCE() {
		return this.INSTANCE;
	}
}
    
