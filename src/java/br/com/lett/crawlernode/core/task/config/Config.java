package br.com.lett.crawlernode.core.task.config;

import java.util.List;

import br.com.lett.crawlernode.core.fetcher.FetchMode;

public class Config {
	
	/**
	 * The list of proxy services to be used.
	 */
	protected List<String> proxies;
	
	/**
	 * The maximum number of connection attempts during page fetching.
	 */
	protected int connectionAttempts;
	
	/**
	 * Defines the fetcher to be used on the the first fetching of the crawling.
	 * The result of this fetching will be passed to the crawler extraction method.
	 */
	protected FetchMode fetcher;
	
	
	public Config() {
		super();
	}
	
	public void setFetcher(FetchMode fetcher) {
		this.fetcher = fetcher;
	}
	
	public FetchMode getFetcher() {
		return this.fetcher;
	}
	
	public void setProxyList(List<String> proxies) {
		this.proxies = proxies;
	}
	
	public List<String> getProxyList() {
		return this.proxies;
	}
	
	public void setConnectionAttempts(int connectionAttempts) {
		this.connectionAttempts = connectionAttempts;
	}
	
	public int getConnectionAttempts() {
		return this.connectionAttempts;
	}

}
