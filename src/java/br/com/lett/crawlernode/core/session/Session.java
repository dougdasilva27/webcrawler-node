package br.com.lett.crawlernode.core.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.DateConstants;

public class Session {

  protected static final Logger logger = LoggerFactory.getLogger(Session.class);

  protected DateTime date = new DateTime(DateConstants.timeZone);

  protected String taskStaus;

  /** Id of current crawling session. It's the same id of the message from Amazon SQS */
  protected String sessionId;

  /** Name of the queue from which the message was retrieved */
  protected String queueName;

  /** Original URL of the sku being crawled */
  protected String originalURL;

  /** Association of URL and its final modified version, a redirection for instance */
  Map<String, String> redirectionMap;

  /** Association of URL and its proxy */
  protected Map<String, LettProxy> requestProxyMap;

  /** Market associated with this session */
  protected Market market;

  /** Errors occurred during crawling session */
  protected List<SessionError> crawlerSessionErrors;

  /** The maximum number of connection attempts to be made when crawling normal information */
  protected int maxConnectionAttemptsWebcrawler;

  /** The maximum number of connection attempts to be made when downloading images */
  protected int maxConnectionAttemptsImages;

  /** Response when request product page */
  protected Object productPageResponse;

  protected long startTime;

  /**
   * Default empty constructor
   */
  public Session(Market market) {

    this.startTime = System.currentTimeMillis();

    this.market = market;

    // creating the errors list
    this.crawlerSessionErrors = new ArrayList<>();

    // creating the map of redirections
    this.redirectionMap = new HashMap<>();
    requestProxyMap = new HashMap<>();
    maxConnectionAttemptsWebcrawler = 0;

    for (String proxy : market.getProxies()) {
      maxConnectionAttemptsWebcrawler += Test.proxies.getProxyMaxAttempts(proxy);
    }

    maxConnectionAttemptsImages = 0;
    for (String proxy : market.getImageProxies()) {
      maxConnectionAttemptsImages = maxConnectionAttemptsImages + Test.proxies.getProxyMaxAttempts(proxy);
    }

  }

  public Session(Request request, String queueName, Markets markets) {
    taskStaus = Task.STATUS_COMPLETED;

    this.startTime = System.currentTimeMillis();

    this.queueName = queueName;
    crawlerSessionErrors = new ArrayList<>();
    redirectionMap = new HashMap<>();
    requestProxyMap = new HashMap<>();
    sessionId = request.getMessageId();
    market = markets.getMarket(request.getMarketId());

    if (!(request instanceof CrawlerRankingKeywordsRequest)) {
      originalURL = request.getMessageBody();
    }

    maxConnectionAttemptsWebcrawler = 0;

    if (Main.executionParameters.getUseFetcher()) {
      // for (String proxy : market.getProxies()) {
      // maxConnectionAttemptsWebcrawler += Main.proxies.getProxyMaxAttempts(proxy);
      // }
      // maxConnectionAttemptsWebcrawler++;
      maxConnectionAttemptsWebcrawler = 2;
    } else {
      for (String proxy : market.getProxies()) {
        maxConnectionAttemptsWebcrawler += Main.proxies.getProxyMaxAttempts(proxy);
      }
    }

    maxConnectionAttemptsImages = 0;
    for (String proxy : market.getImageProxies()) {
      maxConnectionAttemptsImages = maxConnectionAttemptsImages + Main.proxies.getProxyMaxAttempts(proxy);
    }

  }

  public long getStartTime() {
    return this.startTime;
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

  public Object getProductPageResponse() {
    return productPageResponse;
  }

  public void setProductPageResponse(Object productPageResponse) {
    this.productPageResponse = productPageResponse;
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

  public LettProxy getRequestProxy(String url) {
    return requestProxyMap.get(url);
  }

  public void addRequestProxy(String url, LettProxy proxy) {
    this.requestProxyMap.put(url, proxy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("sessionId: " + sessionId + "\n");
    sb.append("queueName: " + getQueueName() + "\n");
    sb.append("url: " + originalURL + "\n");
    sb.append("marketId: " + market.getNumber() + "\n");

    return sb.toString();
  }

}
