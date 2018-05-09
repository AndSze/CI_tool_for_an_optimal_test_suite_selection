package messages;

public enum ServerACKType {
	
	SERVER_INFO_OK (0),
	SERVER_INFO_NOT_OK (1),
	MEASUREMENTDATA (2),
	MEASUREMENTHISTORY(3);

	private int ack_type;

	private ServerACKType(int ack_type) 
	{
		this.ack_type = ack_type;
	}

	public int getACKType() 
	{
		return this.ack_type;
	}

	public void setACKType(int ack_type) 
	{
		this.ack_type = ack_type;
	}
}
