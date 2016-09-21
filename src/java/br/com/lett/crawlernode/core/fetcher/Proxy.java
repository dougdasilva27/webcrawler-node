package br.com.lett.crawlernode.core.fetcher;

/**
 * Processor request utils
 * 
 * @author samirleao
 *
 */

public class Proxy {

	private String 	host;
	private String 	user; 
	private String 	pass;
	private Integer port;

	public Proxy(String host, Integer port, String user, String pass) {
		super();
		this.host = host;
		this.port = port;
		this.user = user;
		this.pass = pass;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
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