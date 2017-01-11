package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.queue.QueueService;

public class Session {

	protected static final Logger logger = LoggerFactory.getLogger(Session.class);

	public static final String DISCOVERY_TYPE 	= "discovery";
	public static final String SEED_TYPE 		= "seed";
	public static final String INSIGHTS_TYPE 	= "insights";
	public static final String TEST_TYPE 		= "test";
	
	protected DateTime date = new DateTime(DateTimeZone.forID("America/Sao_Paulo"));

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

	/** Original URL of the sku being crawled */
	protected String originalURL;

	/** Association of URL and its final modified version, a redirection for instance */
	Map<String, String> redirectionMap;

	/** Market associated with this session */
	protected Market market;

	/** Errors occurred during crawling session */
	protected ArrayList<SessionError> crawlerSessionErrors;
	
	/** The maximum number of connection attempts to be made when crawling normal information */
	protected int maxConnectionAttemptsWebcrawler;
	
	/** The maximum number of connection attempts to be made when downloading images */
	protected int maxConnectionAttemptsImages;


	/**
	 * Default empty constructor
	 */
	public Session() {
		super();

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<SessionError>();

		// creating the map of redirections
		this.redirectionMap = new HashMap<String, String>();
		
	}

	public Session(Message message, String queueName, Markets markets) {
		Map<String, MessageAttributeValue> attrMap = message.getMessageAttributes();

		// setting queue name
		this.queueName = queueName;

		// creating the errors list
		this.crawlerSessionErrors = new ArrayList<SessionError>();

		// creating the map of redirections
		this.redirectionMap = new HashMap<String, String>();

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
		
		maxConnectionAttemptsWebcrawler = 0;
		for (String proxy : market.getProxies()) {
			maxConnectionAttemptsWebcrawler = maxConnectionAttemptsWebcrawler + Main.proxies.getProxyMaxAttempts(proxy);
		}
		
		maxConnectionAttemptsImages = 0;
		for (String proxy : market.getImageProxies()) {
			maxConnectionAttemptsImages = maxConnectionAttemptsImages + Main.proxies.getProxyMaxAttempts(proxy);
		}

		// setting URL and originalURL
		this.originalURL = message.getBody();
	}
	
	public DateTime getDate() {
		return this.date;
	}
	
	public int getMaxConnectionAttemptsCrawler() {
		return this.maxConnectionAttemptsWebcrawler;
	}
	
	public void setMaxConnectionAttemptsCrawler(int maxConnectionAttemptsWebcrawler) {
		this.maxConnectionAttemptsWebcrawler = maxConnectionAttemptsWebcrawler;
	}
	
	public int getMaxConnectionAttemptsImages() {
		return this.maxConnectionAttemptsImages;
	}
	
	public void setMaxConnectionAttemptsImages(int maxConnectionAttemptsImages) {
		this.maxConnectionAttemptsImages = maxConnectionAttemptsImages;
	}

	public String getInternalId() {
		/* by default returns an empty string */
		return "";
	}

	public Long getProcessedId() {
		/* by default returns a null object */
		return null;
	}

	public String getOriginalURL() {
		return originalURL;
	}

	public void setOriginalURL(String originalURL) {
		this.originalURL = originalURL;
	}

	public void addRedirection(String originalURL, String redirectedURL) {
		this.redirectionMap.put(originalURL, redirectedURL);
	}

	public String getRedirectedToURL(String originalURL) {
		return this.redirectionMap.get(originalURL);
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

	public void clearSession() {
		/* do nothing by default */
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("session id: " + sessionId + "\n");
		sb.append("queue name: " + getQueueName() + "\n");
		sb.append("url: " + originalURL + "\n");
		sb.append("market id: " + market.getNumber() + "\n");
		sb.append("market name: " + market.getName() + "\n");
		sb.append("market city: " + market.getCity() + "\n");

		return sb.toString();
	}

	public ArrayList<SessionError> getErrors() {
		return crawlerSessionErrors;
	}

	public void registerError(SessionError error) {
		crawlerSessionErrors.add(error);
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

}
