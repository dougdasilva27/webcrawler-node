package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.main.Main;

public class Session {

	protected static final Logger logger = LoggerFactory.getLogger(Session.class);

	public static final String DISCOVERY_TYPE 	= "discovery";
	public static final String SEED_TYPE 		= "seed";
	public static final String INSIGHTS_TYPE 	= "insights";
	public static final String TEST_TYPE 		= "test";

	protected DateTime date = new DateTime(DateTimeZone.forID("America/Sao_Paulo"));
	
	protected String taskStaus;

	/** Id of current crawling session. It's the same id of the message from Amazon SQS */
	protected String sessionId;

	/** Name of the queue from which the message was retrieved */
	protected String queueName;

	/** Original URL of the sku being crawled */
	protected String originalURL;

	/** Association of URL and its final modified version, a redirection for instance */
	Map<String, String> redirectionMap;

	/** Market associated with this session */
	protected Market market;

	/** Errors occurred during crawling session */
	protected List<SessionError> crawlerSessionErrors;

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
		this.crawlerSessionErrors = new ArrayList<>();

		// creating the map of redirections
		this.redirectionMap = new HashMap<>();

	}

	public Session(Request request, String queueName, Markets markets) {
		
		taskStaus = Task.STATUS_COMPLETED;

		// setting queue name
		this.queueName = queueName;

		// creating the errors list
		crawlerSessionErrors = new ArrayList<>();

		// creating the map of redirections
		redirectionMap = new HashMap<>();

		// setting session id
		sessionId = request.getMessageId();

		// setting Market
		String city = request.getCityName();

		String name = request.getMarketName();

		// setting URL and originalURL
		this.originalURL = request.getMessageBody();

		if (city != null && name != null) {
			market = markets.getMarket(city, name);
		}

		maxConnectionAttemptsWebcrawler = 0;
		for (String proxy : market.getProxies()) {
			maxConnectionAttemptsWebcrawler = maxConnectionAttemptsWebcrawler + Main.proxies.getProxyMaxAttempts(proxy);
		}

		maxConnectionAttemptsImages = 0;
		for (String proxy : market.getImageProxies()) {
			maxConnectionAttemptsImages = maxConnectionAttemptsImages + Main.proxies.getProxyMaxAttempts(proxy);
		}
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
	
	public String getTaskStatus() {
		return taskStaus;
	}
	
	public void setTaskStatus(String taskStatus) {
		this.taskStaus = taskStatus;
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

	public List<SessionError> getErrors() {
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
