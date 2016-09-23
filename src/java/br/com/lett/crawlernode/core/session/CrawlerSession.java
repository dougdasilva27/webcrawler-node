package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.sqs.model.Message;

import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.server.QueueHandler;

public class CrawlerSession {

	public static final String DISCOVERY_TYPE 	= "discovery";
	public static final String SEED_TYPE 		= "seed";
	public static final String INSIGHTS_TYPE 	= "insights";
	public static final String TEST_TYPE 		= "test";

	/** Id of current crawling session. It's the same id of the message from Amazon SQS */
	protected String sessionId;

	/** Name of the queue from which the message was retrieved */
	protected String queueName;

	/**
	 * A receipt handle used to delete a message from the Amazon sqs
	 * The id is useful to identify the message, but the it can't be used to delete the message.
	 * Only the messageReceiptHandle can be used for this. It's a string received at the moment
	 * we get the message from the queue 
	 */
	protected String messageReceiptHandle;

	/** Type of crawler session: discovery | insights */
	protected String type;

	/** Base original URL */
	protected String originalURL;

	/** Sku url being crawled */
	protected String url;

	/** Market associated with this session */
	protected Market market;

	/** Map associating an URL with the number of requests for this URL */
	protected Map<String, Integer> urlRequests;

	/** Map associating an URL with the last proxy used to request this URL */
	protected Map<String, LettProxy> lastURLRequest;

	/** Errors ocurred during crawling session */
	protected ArrayList<CrawlerSessionError> crawlerSessionErrors;
	

	/**
	 * Default empty constructor
	 */
	public CrawlerSession() {
		super();
	}
	
	public CrawlerSession(Message message, String queueName) {
		
		// setting queue name
		this.queueName = queueName;

		// creating the urlRequests map
		this.urlRequests = new HashMap<String, Integer>();

		this.lastURLRequest = new HashMap<String, LettProxy>();

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<CrawlerSessionError>();

		// setting session id
		this.sessionId = message.getMessageId();

		// setting message receipt handle
		this.setMessageReceiptHandle(message.getReceiptHandle());

		// setting Market
		this.market = new Market(message);

		// setting URL and originalURL
		this.url = message.getBody();
		this.originalURL = message.getBody();

		// type
		if (queueName.equals(QueueHandler.INSIGHTS) || queueName.equals(QueueHandler.INSIGHTS_DEAD)) {
			this.type = INSIGHTS_TYPE;
		}
		else if (queueName.equals(QueueHandler.SEED) || queueName.equals(QueueHandler.SEED_DEAD)) {
			this.type = SEED_TYPE;
		}
		else if (queueName.equals(QueueHandler.DISCOVER) || queueName.equals(QueueHandler.DISCOVER_DEAD)) {
			this.type = DISCOVERY_TYPE;
		}
		else if (queueName.equals(QueueHandler.DEVELOPMENT)) {
			this.type = INSIGHTS_TYPE; // it's supposed to be the same as insights
		}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessageReceiptHandle() {
		return messageReceiptHandle;
	}

	public void setMessageReceiptHandle(String messageReceiptHandle) {
		this.messageReceiptHandle = messageReceiptHandle;
	}

	public Map<String, Integer> getUrlRequest() {
		return urlRequests;
	}

	public void setUrlRequest(Map<String, Integer> urlRequest) {
		this.urlRequests = urlRequest;
	}
	
	public int getVoidAttempts() {
		/* returns -1 by default */
		return -1;
	}
	
	public int getTrucoAttempts() {
		/* returns -1 by default */
		return -1;
	}
	
	public void incrementVoidAttemptsCounter() {
		/* do nothing by default */
	}
	
	public void incrementTrucoAttemptsCounter() {
		/* do nothing by default */
	}

	public void addRequestInfo(String url) {
		if (urlRequests.containsKey(url)) {
			urlRequests.put(url, urlRequests.get(url) + 1);
		} else {
			urlRequests.put(url, 1);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("session id: " + this.sessionId + "\n");
		sb.append("queue name: " + this.getQueueName() + "\n");
		sb.append("type: " + this.type + "\n");
		sb.append("original url: " + this.originalURL + "\n");
		sb.append("url: " + this.url + "\n");
		sb.append("market id: " + this.market.getNumber() + "\n");
		sb.append("market name: " + this.market.getName() + "\n");
		sb.append("market city: " + this.market.getCity() + "\n");

		return sb.toString();
	}

	public void addProxyRequestInfo(String url, LettProxy proxy) {
		this.lastURLRequest.put(url, proxy);
	}

	public Map<String, LettProxy> getLastURLRequest() {
		return lastURLRequest;
	}

	public void setLastURLRequest(Map<String, LettProxy> lastURLRequest) {
		this.lastURLRequest = lastURLRequest;
	}

	public ArrayList<CrawlerSessionError> getErrors() {
		return crawlerSessionErrors;
	}

	public void registerError(CrawlerSessionError error) {
		crawlerSessionErrors.add(error);
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

}
