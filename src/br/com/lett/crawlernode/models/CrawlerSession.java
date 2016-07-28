package br.com.lett.crawlernode.models;

import java.util.Map;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.queueservice.QueueService;

public class CrawlerSession {
	
	public static final String STANDALONE = "standalone";
	public static final String INSIGHTS = "insights";
	
	/**
	 * id of current crawling session. It's the same id of the message from Amazon SQS
	 */
	private String sessionId;
	
	/**
	 * A receipt handle used to delete a message from the Amazon sqs
	 * The id is useful to identify the message, but the it can't be used to delete the message.
	 * Only the messageReceiptHandle can be used for this. It's a string received at the moment
	 * we get the message from the queue 
	 */
	private String messageReceiptHandle;
	
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
	
	
	public CrawlerSession(Message message) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();
		
		// setting truco attempts
		this.trucoAttempts = 0;
		
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
			this.processedId = Integer.parseInt(attrMap.get(QueueService.PROCESSED_ID_MESSAGE_ATTR).getStringValue());
		}
		
	}
	
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("session id: " + this.sessionId + "\n");
		sb.append("type: " + this.type + "\n");
		sb.append("seed id: " + this.seedId + "\n");
		sb.append("original url: " + this.originalURL + "\n");
		sb.append("url: " + this.url + "\n");
		sb.append("processed id: " + this.processedId + "\n");
		sb.append("market id: " + this.market.getNumber() + "\n");
		sb.append("market name: " + this.market.getName() + "\n");
		sb.append("market city: " + this.market.getCity() + "\n");

		return sb.toString();
	}

	public String getMessageReceiptHandle() {
		return messageReceiptHandle;
	}

	public void setMessageReceiptHandle(String messageReceiptHandle) {
		this.messageReceiptHandle = messageReceiptHandle;
	}

}
