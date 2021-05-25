package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.DateUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.ScraperInformation;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Session {

   protected static final Logger logger = LoggerFactory.getLogger(Session.class);

   protected List<String> proxies = new ArrayList<>();
   protected List<String> imageProxies = new ArrayList<>();

   protected DateTime date = new DateTime(DateUtils.timeZone);

   protected List<String> responseBodiesPath = new ArrayList<>();

   protected String taskStaus;

   /**
    * Id of current crawling session. It's the same id of the message from Amazon SQS
    */
   protected String sessionId;

   /**
    * Name of the queue from which the message was retrieved
    */
   protected String queueName;

   /**
    * Original URL of the sku being crawled
    */
   protected String originalURL;

   /**
    * Association of URL and its final modified version, a redirection for instance
    */
   Map<String, String> redirectionMap;

   /**
    * Association of URL and its proxy
    */
   protected Map<String, LettProxy> requestProxyMap;

   /**
    * Market associated with this session
    */
   protected Market market;

   /**
    * Supplier Id associated with this session
    */
   protected Long supplierId;

   /**
    * Errors occurred during crawling session
    */
   protected List<SessionError> crawlerSessionErrors;

   /**
    * The maximum number of connection attempts to be made when crawling normal information
    */
   protected int maxConnectionAttemptsWebcrawler;


   /**
    * Response when request product page
    */
   protected Object productPageResponse;

   protected long startTime;

   protected JSONObject options;

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

   }

   public Session(Request request, String queueName, Market market) {
      taskStaus = Task.STATUS_COMPLETED;

      this.startTime = System.currentTimeMillis();

      this.queueName = queueName;
      crawlerSessionErrors = new ArrayList<>();
      redirectionMap = new HashMap<>();
      requestProxyMap = new HashMap<>();
      sessionId = request.getMessageId();
      this.market = market;
      supplierId = request.getSupplierId();

      this.options = request.getOptions();

      JSONArray proxiesArray = this.options.optJSONArray("proxies");
      if (proxiesArray != null && !proxiesArray.isEmpty()) {
         for (Object o : proxiesArray) {
            String proxy = (String) o;
            proxies.add(proxy);
         }
      } else {
         proxies = Arrays.asList(ProxyCollection.BUY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NO_PROXY);
      }

      JSONArray imageProxiesArray = this.options.optJSONArray("proxies");
      if (imageProxiesArray != null && !imageProxiesArray.isEmpty()) {
         for (Object o : imageProxiesArray) {
            String proxy = (String) o;
            imageProxies.add(proxy);
         }
      } else {
         imageProxies = Arrays.asList(ProxyCollection.BUY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NO_PROXY);
      }


      if (!(request instanceof CrawlerRankingKeywordsRequest)) {
         originalURL = request.getParameter();
      }

      maxConnectionAttemptsWebcrawler = 0;

      if (GlobalConfigurations.executionParameters.getUseFetcher()) {
         // for (String proxy : market.getProxies()) {
         // maxConnectionAttemptsWebcrawler += Main.proxies.getProxyMaxAttempts(proxy);
         // }
         // maxConnectionAttemptsWebcrawler++;
         maxConnectionAttemptsWebcrawler = 2;
      } else {
         for (String proxy : this.proxies) {
            maxConnectionAttemptsWebcrawler += GlobalConfigurations.proxies.getProxyMaxAttempts(proxy);
         }
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

   public String getInternalId() {
      /* by default returns an empty string */
      return "";
   }

   public Long getProcessedId() {
      /* by default returns a null object */
      return null;
   }

   public List<String> getResponseBodiesPath() {
      return responseBodiesPath;
   }

   public void setResponseBodiesPath(List<String> responseBodiesPath) {
      this.responseBodiesPath = responseBodiesPath;
   }

   public void addResponseBodyPath(String path) {
      this.responseBodiesPath.add(path);
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

   public Long getSupplierId() {
      return supplierId;
   }

   public void setSupplierId(Long supplierId) {
      this.supplierId = supplierId;
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

   public Object getProductPageResponse() {
      return productPageResponse;
   }

   public void setProductPageResponse(Object productPageResponse) {
      this.productPageResponse = productPageResponse;
   }

   public void incrementVoidAttemptsCounter() {
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

   public void setProxies(ArrayList<String> proxies) {
      this.proxies = proxies;
   }

   public List<String> getProxies() {
      return this.proxies;
   }

   public JSONObject getOptions() {
      return options;
   }

   public void setOptions(JSONObject options) {
      this.options = options;
   }

   public List<String> getImageProxies() {
      return imageProxies;
   }

   public void setImageProxies(List<String> imageProxies) {
      this.imageProxies = imageProxies;
   }
}
