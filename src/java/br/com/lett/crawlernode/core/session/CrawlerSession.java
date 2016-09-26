package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.server.QueueService;

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

	/** Sku url being crawled */
	protected String url;

	/** Market associated with this session */
	protected Market market;

	/** Errors ocurred during crawling session */
	protected ArrayList<CrawlerSessionError> crawlerSessionErrors;


	/**
	 * Default empty constructor
	 */
	public CrawlerSession() {
		super();
	}

	public CrawlerSession(Message message, String queueName, Markets markets) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		// setting queue name
		this.queueName = queueName;

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<CrawlerSessionError>();

		// setting session id
		this.sessionId = message.getMessageId();

		// setting message receipt handle
		this.setMessageReceiptHandle(message.getReceiptHandle());

		// setting Market
		String city = null;
		String name = null;
		if (attrMap.containsKey(QueueService.CITY_MESSAGE_ATTR) && attrMap.containsKey(QueueService.MARKET_MESSAGE_ATTR)) {
			city = attrMap.get(QueueService.CITY_MESSAGE_ATTR).getStringValue();
			name = attrMap.get(QueueService.MARKET_MESSAGE_ATTR).getStringValue();
			this.market = markets.getMarket(city, name);
		}
	
		// setting URL and originalURL
		this.url = message.getBody();

	}

	public String getInternalId() {
		/* by default returns an empty string */
		return "";
	}

	public Long getProcessedId() {
		/* by default returns a null object */
		return null;
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

	public String getMessageReceiptHandle() {
		return messageReceiptHandle;
	}

	public void setMessageReceiptHandle(String messageReceiptHandle) {
		this.messageReceiptHandle = messageReceiptHandle;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("session id: " + this.sessionId + "\n");
		sb.append("queue name: " + this.getQueueName() + "\n");
		sb.append("url: " + this.url + "\n");
		sb.append("market id: " + this.market.getNumber() + "\n");
		sb.append("market name: " + this.market.getName() + "\n");
		sb.append("market city: " + this.market.getCity() + "\n");

		return sb.toString();
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
