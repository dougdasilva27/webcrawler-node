package br.com.lett.crawlernode.kernel.task;

import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.kernel.fetcher.Cookies;

public class Cache {
	
	/** A map associating each crawler with the cookies used to make request */
	private Map<String, Cookies> crawlerCookiesMap;
	
	public Cache() {
		this.crawlerCookiesMap = new HashMap<String, Cookies>();
	}
	
	public Cookies getCrawlerCookies(String crawler) {
		return this.crawlerCookiesMap.get(crawler);
	}
	
	public void updateCookiesMap(String crawler, Cookies cookies) {
		this.crawlerCookiesMap.put(crawler, cookies);
	}
	
	

}
