package br.com.lett.crawlernode.models;

public class CrawlerSession {
	
	/**
	 * id of current crawling session
	 */
	private String sessionId;
	
	/**
	 * sku url being crawled
	 */
	private String url;
	
	/**
	 * processed id associated with the sku being crawled
	 */
	private int processedId;
	
	
	public CrawlerSession() {
		super();
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String getSessionId() {
		return sessionId;
	}


	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}


	public int getProcessedId() {
		return processedId;
	}


	public void setProcessedId(int processedId) {
		this.processedId = processedId;
	}

}
