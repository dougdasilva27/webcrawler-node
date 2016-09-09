package br.com.lett.crawlernode.kernel.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.kernel.fetcher.LettProxy;
import br.com.lett.crawlernode.kernel.models.Market;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.server.QueueHandler;
import br.com.lett.crawlernode.server.QueueService;
import br.com.lett.crawlernode.server.RequestMessageResult;
import br.com.lett.crawlernode.server.SQSRequestResult;

public class CrawlerSession {

	public static final String DISCOVERY_TYPE = "discovery";
	public static final String SEED_TYPE = "seed";
	public static final String INSIGHTS_TYPE = "insights";

	/** Id of current crawling session. It's the same id of the message from Amazon SQS */
	private String sessionId;

	/** Name of the queue from which the message was retrieved */
	private String queueName;

	/**
	 * A receipt handle used to delete a message from the Amazon sqs
	 * The id is useful to identify the message, but the it can't be used to delete the message.
	 * Only the messageReceiptHandle can be used for this. It's a string received at the moment
	 * we get the message from the queue 
	 */
	private String messageReceiptHandle;

	/** Type of crawler session: discovery | insights */
	private String type;

	/** Current url seed id */
	private String seedId;

	/** Base original URL */
	private String originalURL;

	/** Sku url being crawled */
	private String url;

	/** Processed id associated with the sku being crawled */
	private Long processedId;

	/** Internal id associated with the sku being crawled */
	private String internalId;

	/** Market associated with this session */
	private Market market;

	/** Number of truco checks */
	private int trucoAttemptsCounter;

	/** Number of readings to prevent a void status */
	private int voidAttemptsCounter;

	/** Map associating an URL with the number of requests for this URL */
	private Map<String, Integer> urlRequests;

	/** Map associating an URL with the last proxy used to request this URL */
	private Map<String, LettProxy> lastURLRequest;

	/** Errors ocurred during crawling session */
	private ArrayList<CrawlerSessionError> crawlerSessionErrors;

	/**
	 * Default constructor to be used when running in production.
	 * @param message with informations to create a new crawler session
	 */
	public CrawlerSession(Message message) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
			this.queueName = QueueHandler.DEVELOPMENT;
		} else {
			this.queueName = QueueHandler.INSIGHTS;
		}

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

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

		// setting processed id
		if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
			this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		}

		// setting internal id
		if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
			this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		}

		// type
		if (this.internalId != null && this.processedId != null) {
			this.type = INSIGHTS_TYPE;
		} else {
			this.type = DISCOVERY_TYPE;
		}

		//		if (Main.executionParameters.getMode().equals(ExecutionParameters.MODE_INSIGHTS)) {
		//
		//			// setting internal id
		//			if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
		//				this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		//			}
		//
		//			// setting processed id
		//			if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
		//				this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		//			}
		//
		//			// setting session type
		//			this.type = INSIGHTS_TYPE;
		//		} 
		//		else {
		//			this.type = DISCOVERY_TYPE;
		//		}

	}

	public CrawlerSession(Message message, String queueName) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
		
		// setting queue name
		this.queueName = queueName;

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

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

		// setting processed id
		if (attrMap.containsKey(QueueService.PROCESSED_ID_MESSAGE_ATTR)) {
			this.processedId = Long.parseLong(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		}

		// setting internal id
		if (attrMap.containsKey(QueueService.INTERNAL_ID_MESSAGE_ATTR)) {
			this.internalId = attrMap.get(QueueService.INTERNAL_ID_MESSAGE_ATTR).getStringValue();
		}

		// type
		if (queueName.equals(QueueHandler.INSIGHTS)) {
			this.type = INSIGHTS_TYPE;
		}
		else if (queueName.equals(QueueHandler.SEED)) {
			this.type = SEED_TYPE;
		}
		else if (queueName.equals(QueueHandler.DISCOVER)) {
			this.type = DISCOVERY_TYPE;
		}
		else if (queueName.equals(QueueHandler.DEVELOPMENT)) {
			this.type = INSIGHTS_TYPE; // it's supposed to be the same as insights
		}

	}

	public CrawlerSession(String url, Market market) {

		if (Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_DEVELOPMENT)) {
			this.queueName = QueueHandler.DEVELOPMENT;
		} else {
			this.queueName = QueueHandler.INSIGHTS;
		}

		// initialize counters
		this.trucoAttemptsCounter = 0;
		this.voidAttemptsCounter = 0;

		// creating the urlRequests map
		this.urlRequests = new HashMap<String, Integer>();

		this.lastURLRequest = new HashMap<String, LettProxy>();

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<CrawlerSessionError>();

		// setting session id
		this.sessionId = "test";

		// setting Market
		this.market = market;

		// setting URL and originalURL
		this.url = url;
		this.originalURL = url;

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

	public Long getProcessedId() {
		return processedId;
	}

	public void setProcessedId(Long processedId) {
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
		return trucoAttemptsCounter;
	}

	public void incrementTrucoAttemptsCounter() {
		this.trucoAttemptsCounter++;
	}

	public void incrementVoidAttemptsCounter() {
		this.voidAttemptsCounter++;
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

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public Map<String, Integer> getUrlRequest() {
		return urlRequests;
	}

	public void setUrlRequest(Map<String, Integer> urlRequest) {
		this.urlRequests = urlRequest;
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
		sb.append("seed id: " + this.seedId + "\n");
		sb.append("original url: " + this.originalURL + "\n");
		sb.append("url: " + this.url + "\n");
		sb.append("processed id: " + this.processedId + "\n");
		sb.append("internal id: " + this.internalId + "\n");
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

	public int getVoidAttempts() {
		return voidAttemptsCounter;
	}

	public void setVoidAttempts(int voidAttempts) {
		this.voidAttemptsCounter = voidAttempts;
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
