package br.com.lett.crawlernode.core.fetcher;


public class LettProxy {
	
	private String 	source;
	private String 	address;
	private Integer port;
	private String 	location;
	private String 	user;
	private String 	pass;
	
	
	public LettProxy(String source, 
			String address, 
			Integer port,
			String location, 
			String user, 
			String pass) 
	{
		super();
		this.source = source;
		this.address = address;
		this.port = port;
		this.location = location;
		this.user = user;
		this.pass = pass;
	}
		
	@Override
	public String toString() {
		return "Proxy [source=" + source + ", address=" + address + ", port="
				+ port + ", location=" + location + ", user=" + user
				+ ", pass=" + pass + "]";
	}

	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	
	public String getPass() {
		return pass;
	}
	public void setPass(String pass) {
		this.pass = pass;
	}
	
}
