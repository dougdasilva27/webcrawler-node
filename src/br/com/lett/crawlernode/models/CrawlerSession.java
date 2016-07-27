package br.com.lett.crawlernode.models;

public class CrawlerSession {
	
	public static final String STANDALONE = "standalone";
	public static final String INSIGHTS = "insights";
	
	/**
	 * id of current crawling session
	 */
	private String sessionId;
	
	/**
	 * type of crawler session: standalone | insights
	 */
	private String type;
	
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
	 * number of truco checks
	 */
	private int trucoAttempts;
	
	
	public CrawlerSession() {
		super();
		trucoAttempts = 0;
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

	public int getTrucoAttempts() {
		return trucoAttempts;
	}

	public void incrementTrucoAttempts() {
		this.trucoAttempts++;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
