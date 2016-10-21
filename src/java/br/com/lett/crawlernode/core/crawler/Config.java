package br.com.lett.crawlernode.core.crawler;

import java.util.List;

public class Config {
	
	private List<String> proxies;
	
	private int connectionAttempts;
	
	public Config() {
		super();
	}
	
	public void setProxyList(List<String> proxies) {
		this.proxies = proxies;
	}
	
	public void setConnectionAttempts(int connectionAttempts) {
		this.connectionAttempts = connectionAttempts;
	}

}
