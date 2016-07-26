package br.com.lett.crawlernode.models;

public class CrawlerSession {
	
	/**
	 * id of current crawling session
	 */
	private String sessionId;
	
	/**
	 * Current url seed id
	 */
	private String seedId;
	
	/**
	 * base original URL
	 */
	private String originalURL;
	
	/**
	 * sku url being crawled
	 */
	private String url;
	
	/**
	 * processed id associated with the sku being crawled
	 */
	private int processedId;
	
	/**
	 * the market associated with this session
	 */
	private Market market;
	
	/**
	 * processed model of truco mode
	 * this is the information of the current product of a previous crawling
	 * the data that was saved on database before the current reading
	 */
	private ProcessedModel truco;
	
	
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

	public Market getMarket() {
		return market;
	}

	public void setMarket(Market market) {
		this.market = market;
	}

	public ProcessedModel getTruco() {
		return truco;
	}

	public void setTruco(ProcessedModel truco) {
		this.truco = truco;
	}

	public String getOriginalURL() {
		return originalURL;
	}


	public void setOriginalURL(String originalURL) {
		this.originalURL = originalURL;
	}

	public String getSeedId() {
		return seedId;
	}

	public void setSeedId(String seedId) {
		this.seedId = seedId;
	}

}
