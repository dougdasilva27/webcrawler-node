package br.com.lett.crawlernode.core.task.impl;

import br.com.lett.crawlernode.aws.dynamodb.Dynamo;
import br.com.lett.crawlernode.aws.kinesis.KPLProducer;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.*;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.RequestMethod;
import br.com.lett.crawlernode.core.session.SentinelCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.*;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.config.CrawlerConfig;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.dto.ProductDTO;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.exceptions.NotFoundProductException;
import br.com.lett.crawlernode.exceptions.RequestException;
import br.com.lett.crawlernode.exceptions.ResponseCodeException;
import br.com.lett.crawlernode.integration.redis.CrawlerCache;
import br.com.lett.crawlernode.integration.redis.config.RedisDb;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.metrics.Exporter;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.TestHtmlBuilder;
import models.Offer;
import models.Offers;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static br.com.lett.crawlernode.core.fetcher.models.Response.ResponseBuilder;
import static br.com.lett.crawlernode.main.GlobalConfigurations.executionParameters;

/**
 * The Crawler superclass. All crawler tasks must extend this class to override both the shouldVisit and extract methods.
 *
 * @author Samir Leao
 */


public abstract class Crawler extends Task {

   protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);
   private String crawledInternalId;

   protected static final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|ico|tiff?|mid|mp2|mp3|mp4"
      + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))(\\?.*)?$");

   /**
    * Maximum attempts during active void analysis It's essentially the number of times that we will rerun the extract method to crawl a product from a page
    */
   protected static final int MAX_VOID_ATTEMPTS = 3;

   protected DataFetcher dataFetcher;

   protected CrawlerConfig config;

   protected CrawlerWebdriver webdriver;

   private static final CrawlerCache cacheClient = new CrawlerCache(RedisDb.CRAWLER);

   /**
    * Cookies that must be used to fetch the sku page this attribute is set by the handleCookiesBeforeFetch method.
    */
   protected List<Cookie> cookies;

   protected Set<org.openqa.selenium.Cookie> cookiesWD = new HashSet<>();

   protected final CacheConfig cacheConfig = new CacheConfig();

   protected Crawler(Session session) {
      this.session = session;
      this.cookies = new ArrayList<>();

      createDefaultConfig();
   }

   /**
    * Create the config with default values
    */
   private void createDefaultConfig() {
      this.config = new CrawlerConfig();
      this.config.setFetcher(FetchMode.STATIC);
      this.config.setProxyList(new ArrayList<>());
      this.config.setConnectionAttempts(0);
   }

   /**
    * It defines wether the crawler must true to extract data or not.
    *
    * @return
    */
   public boolean shouldVisit() {
      return true;
   }

   /**
    * Set cookies before page fetching
    */
   public void handleCookiesBeforeFetch() {
      /* subclasses must implement */
   }

   private void handleCacheableCookiesBeforeFetch(Boolean force) {
      if (cacheConfig.getRequest() != null && cacheConfig.getRequestMethod() != null) {
         Request request = cacheConfig.getRequest();
         RequestMethod requestMethod = cacheConfig.getRequestMethod();
         cookies = cacheCookies("cookie", requestMethod, request, force);
      }
   }

   /**
    * Performs any desired transformation on the URL before the actual fetching.
    *
    * @param url the URL we want to modify
    * @return the modified URL, that will be used in the fetching
    */
   public String handleURLBeforeFetch(String url) {
      return url;
   }

   /**
    * Contains all the logic to sku information extraction. Must be implemented on subclasses.
    *
    * @param document
    * @return A product with all it's crawled informations
    */
   public List<Product> extractInformation(Document document) throws Exception {
      return new ArrayList<>();
   }

   /**
    * Contains all the logic to sku information extraction. Must be implemented on subclasses.
    *
    * @param json
    * @return A product with all it's crawled informations
    */
   public List<Product> extractInformation(JSONObject json) throws Exception {
      return new ArrayList<>();
   }

   /**
    * Contains all the logic to sku information extraction. Must be implemented on subclasses.
    *
    * @param array
    * @return A product with all it's crawled informations
    */
   public List<Product> extractInformation(JSONArray array) throws Exception {
      return new ArrayList<>();
   }

   @Override
   public void onStart() {
      Logging.printLogInfo(logger, session, "START");
   }

   /**
    * Overrides the run method that will perform a task within a thread. The actual thread performs it's computation controlled by an Executor, from Java's Executors Framework.
    */
   @Override
   public void processTask() {
      try {
         setDataFetcher();

         if (session instanceof TestCrawlerSession) {
            testRun();
         } else {
            productionRun();
         }
      } catch (Exception e) {
         Exporter.collectError(e, session);
         if (session instanceof SeedCrawlerSession) {
            Persistence.updateFrozenServerTask(((SeedCrawlerSession) session), e.getMessage());
         }
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
   }

   private void setDataFetcher() {
      switch (config.getFetcher()) {
         case APACHE:
            dataFetcher = new ApacheDataFetcher();
            break;
         case JAVANET:
            dataFetcher = new JavanetDataFetcher();
            break;
         case FETCHER:
            dataFetcher = new FetcherDataFetcher();
            break;
         case JSOUP:
            dataFetcher = new JsoupDataFetcher();
            break;
         default:
            dataFetcher = Boolean.TRUE.equals(GlobalConfigurations.executionParameters.getUseFetcher()) ? new FetcherDataFetcher() : new ApacheDataFetcher();
            break;
      }
   }

   private void productionRun() {

      sendProgress(50);

      // crawl informations and create a list of products
      List<Product> products = extract();

      sendProgress(75);

      // This happen if a error ocurred on seed scrap information or if the seed is not a product page
      if (session instanceof SeedCrawlerSession && products.isEmpty()) {
         sendProgress(null);
      }

      Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

      // print crawled information
      for (Product product : products) {
         printCrawledInformation(product);
      }

      // insights session
      // there is only one product that will be selected
      // by it's internalId, passed by the crawler session
      if (session instanceof InsightsCrawlerSession || session instanceof ToBuyCrawlerSession) {
         insightsProcess(products);
      }

      // discovery session
      // when processing a task of a suggested URL by the webcrawler or
      // an URL scheduled manually, we won't run active void and
      // we must process each crawled product
      else if (session instanceof DiscoveryCrawlerSession || session instanceof SeedCrawlerSession || session instanceof EqiCrawlerSession) {
         // Before process and save to PostgreSQL
         // we must send the raw crawled data to Kinesis
         if (session instanceof DiscoveryCrawlerSession || session instanceof EqiCrawlerSession) {
            if (!products.isEmpty()) {
               Dynamo.updateReadByCrawlerObjectDynamo(products, session);
            } else {
               Logging.printLogInfo(logger, session, "No products to save in dynamo");
            }
         }

         for (Product p : products) {
            sendToKinesis(p);
         }

         if (session instanceof SeedCrawlerSession) {
            for (Product product : products) {
               processProduct(product);
            }
         }
      }
   }

   private void insightsProcess(List<Product> products) {

      // get crawled product by it's internalId
      Logging.printLogDebug(logger, session, "Selecting product with internalId " + session.getInternalId());
      Product crawledProduct = filter(products, session.getInternalId());

      // if the product is void run the active void analysis
      Product activeVoidResultProduct = crawledProduct;
      if (crawledProduct.isVoid() && !(session instanceof ToBuyCrawlerSession)) {
         Logging.printLogDebug(logger, session, "Product is void...going to start the active void.");
         activeVoidResultProduct = activeVoid(crawledProduct);
      }

      // Before process and save to PostgreSQL
      // we must send the raw crawled data to Kinesis
      sendToKinesis(activeVoidResultProduct);

      if (executionParameters.isSendToKinesisCatalog()) {
         try {
            KPLProducer.sendMessageCatalogToKinesis(crawledProduct, session);
            Logging.printLogDebug(logger, "Sucess to send to kinesis sessionId: " + session.getSessionId());

         } catch (Exception e) {
            Logging.printLogError(logger, "Failed to send to kinesis sessionId: " + session.getSessionId());
         }

      }

   }

   private void sendProgress(Integer progress) {
      if (session instanceof SeedCrawlerSession) {
         if ((progress != null)) {
            Persistence.updateFrozenServerTaskProgress(((SeedCrawlerSession) session), progress);
         } else {
            Persistence.updateFrozenServerTask(((SeedCrawlerSession) session));
         }
      }
   }

   /**
    * Run method to be used when testing
    */
   private void testRun() {

      // crawl informations and create a list of products
      List<Product> products = extract();

      Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

      if (session instanceof TestCrawlerSession) {
         ((TestCrawlerSession) session).setProducts(products);
      }

      for (Product p : products) {

         if (Test.pathWrite != null) {
            String status = getFirstPartyRegexStatus(p);
            printCrawledInformation(p);
            Logging.printLogDebug(logger, session, "Seller Name:" + getFirstPartyRegex(p));
            TestHtmlBuilder.buildProductHtml(new JSONObject(p.toJson()), Test.pathWrite, status, session);
         } else {
            printCrawledInformation(p);
         }
      }
   }

   /**
    * Performs the data extraction of the URL in the session. The four main steps of this method are:
    * <ul>
    * <li>Handle Cookies: Set any necessary cookies to do a proper http request of the page.</li>
    * <li>Handle URL: Makes any necessary modification on the URL in the session, before the
    * request.</li>
    * <li>Fetch: Do a http request and fetch the page data as a DOM.</li>
    * <li>Extraction: Crawl all skus in the URL on the crawling session.</li>
    * </ul>
    *
    * @return An array with all the products crawled in the URL passed by the CrawlerSession, or an empty array list if no product was found.
    */
   public List<Product> extract() {
      List<Product> processedProducts = new ArrayList<>();
      List<Product> products = new ArrayList<>();

      // in cases we are running a truco iteration
      if (webdriver != null && ((RemoteWebDriver) webdriver.driver).getSessionId() != null) {
         webdriver.terminate();
      }

      // handle cookie
      if (cookies.isEmpty()) {
         handleCookiesBeforeFetch();
      }

      // handle URL modifications
      String url = handleURLBeforeFetch(session.getOriginalURL());
      session.setOriginalURL(url);

      try {
         Response response = null;
         Object obj = null;

         Parser parser = this.config.getParser();

         if (parser != Parser.NONE && config.getFetcher() != FetchMode.MIRANHA) {
            response = fetchResponse();
         } else if (config.getFetcher() == FetchMode.MIRANHA) {
            obj = fetchMiranha();
         } else {
            obj = fetch();
            if (obj instanceof Response) {
               response = (Response) obj;
            }
         }
         if (response != null) {
            if (parser == Parser.NONE) {
               parser = Parser.HTML;
            }
            validateResponse(response);
            validateBody(response.getBody(), parser);
            obj = parser.parse(response.getBody());
         }

         session.setProductPageResponse(obj);

         if (obj instanceof Document) {
            products = extractInformation((Document) obj);
         } else if (obj instanceof JSONObject) {
            products = extractInformation((JSONObject) obj);
         } else if (obj instanceof JSONArray) {
            products = extractInformation((JSONArray) obj);
         }
         if(session instanceof SentinelCrawlerSession && products.isEmpty()) {
            throw new NotFoundProductException("No products found in Sentinel");
         }
         for (Product p : products) {
            processedProducts.add(ProductDTO.processCaptureData(p, session));
         }
      } catch (
         ResponseCodeException e) {
         Logging.printLogWarn(logger, session, "ResponseCodeException: " + e.getMessage());
         if (session instanceof SeedCrawlerSession) {
            session.registerError(new SessionError(SessionError.EXCEPTION, e.getMessage()));
         } else if (session instanceof TestCrawlerSession) {
            ((TestCrawlerSession) session).setLastError(e);
         }
      } catch (
         Exception e) {
         Exporter.collectError(e, session);
         if (session instanceof TestCrawlerSession) {
            ((TestCrawlerSession) session).setLastError(e);
         }
         session.registerError(new SessionError(SessionError.EXCEPTION, e.getMessage()));
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));

         return new ArrayList<>();
      }
      return processedProducts;
   }

   private void validateBody(String response, Parser parser) throws RequestException {
      if (parser == Parser.HTML && !Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL).matcher(response).matches()) {
         throw new RequestException("Unexpected body: response is not HTML");
      } else if (parser == Parser.JSON && !response.startsWith("{")) {
         throw new RequestException("Unexpected body: response is not Json");
      } else if (parser == Parser.JSONARRAY && !response.startsWith("[")) {
         throw new RequestException("Unexpected body: response is not Json Array");
      }
   }

   protected void validateResponse(Response response) throws ResponseCodeException {
      if (Integer.toString(response.getLastStatusCode()).charAt(0) != '2' && Integer.toString(response.getLastStatusCode()).charAt(0) != '3') {
         switch (response.getLastStatusCode()) {
            case 403:
               throw new ResponseCodeException("Blocked request", response.getLastStatusCode());
            case 404:
               throw new ResponseCodeException("Requested page not found", response.getLastStatusCode());
            case 400:
               throw new ResponseCodeException("Bad request", response.getLastStatusCode());
            case 500:
               throw new ResponseCodeException("Server error", response.getLastStatusCode());
            case 601:
               throw new ResponseCodeException("Fetcher error timeout", response.getLastStatusCode());
            default:
               throw new ResponseCodeException(response.getLastStatusCode());
         }
      }
   }

   /**
    * Fetch with cache cookie retry logic.
    */
   private Object fetchWithCacheCookie() {
      handleCacheableCookiesBeforeFetch(false);
      Response response = fetchResponse();

      if (!response.isSuccess()) {
         handleCacheableCookiesBeforeFetch(true);
         response = fetchResponse();
      }
      Parser parser = cacheConfig.getParser();
      return parser.parse(response.getBody());
   }

   /**
    * Request the sku URL and parse to a DOM format. This method uses the preferred fetcher according to the crawler configuration. If the fetcher is static, then we use de StaticDataFetcher,
    * Subclasses can override this method for crawl another apis and pages. In Princesadonorte the product page has nothing, but we need the url for crawl this market api.
    * This method is no longer acceptable to fetch page.
    * <p> Use {@link Crawler#fetchResponse()} instead.
    *
    * @return computed time
    * @deprecated (this method does not allow control over the status of requests, forRemoval)
    */
   @Deprecated(forRemoval = true)
   protected Object fetch() {
      String html = "";
      if (config.getFetcher() == FetchMode.WEBDRIVER) {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);

         if (webdriver != null) {
            html = webdriver.getCurrentPageSource();
         }
      } else {
         Request request = RequestBuilder.create().setCookies(cookies).setUrl(session.getOriginalURL()).build();
         Response response = dataFetcher.get(session, request);

         return response;
      }
      return Jsoup.parse(html);
   }

   /**
    * fetch doc as in {@link Crawler#fetch()}, but returns response instead of the body.
    * <h3>Use <em>{@link CrawlerConfig#setParser(Parser)}</em> inside the construct method to set an especific parser</h3>
    *
    * @return the response returned for the request
    */
   protected Response fetchResponse() {
      Response resp = null;

      if (config.getFetcher() == FetchMode.WEBDRIVER) {
         webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);

         if (webdriver != null) {
            resp = ResponseBuilder.create().setBody(webdriver.getCurrentPageSource()).build();
         }
      } else {
         resp = dataFetcher.get(session, RequestBuilder.create().setCookies(cookies).setUrl(session.getOriginalURL()).build());
      }

      return resp;
   }

   protected Document fetchMiranha() {
      return S3Service.fetchHtml(session, session.getFileS3Miranha(), GlobalConfigurations.executionParameters.getBucketMiranha());
   }

   /**
    * Get only the product with the desired internalId.
    *
    * @param products
    * @param desiredInternalId
    * @return The product with the desired internal id, or an empty product if it was not found.
    */
   private Product filter(List<Product> products, String desiredInternalId) {
      for (Product product : products) {
         String crawledInternalId = product.getInternalId();
         if (crawledInternalId != null && crawledInternalId.equals(desiredInternalId)) {
            return product;
         }
      }

      Logging.printLogDebug(logger, session, "Product with internalId " + desiredInternalId + " was not found.");
      return new Product();
   }

   /**
    * This method performs an active analysis of the void status
    *
    * @param product the crawled product
    * @return The resultant product from the analysis
    */
   private Product activeVoid(Product product) {

      if (session.scheduledProductIsVoid()) {

         Logging.printLogDebug(logger, session, "The previous processed is also void. Finishing active void.");
         return product;
      }

      Logging.printLogDebug(logger, session, "The previous processed is not void, starting active void attempts...");

      // starting the active void iterations
      // until a maximum number of attempts, we will rerun the extract
      // method and check if the newly extracted product is void
      // in case it isn't, the loop interrupts and returns the product
      // when attempts reach it's maximum, we interrupt the loop and return the last extracted
      // product, even if it's void
      Product currentProduct = product;
      while (session.getVoidAttempts() < MAX_VOID_ATTEMPTS) {
         session.incrementVoidAttemptsCounter();

         Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPT]" + session.getVoidAttempts());
         List<Product> products = extract();
         currentProduct = filter(products, session.getInternalId());

         if (!currentProduct.isVoid()) {
            Logging.printLogDebug(logger, session, "Product is not void anymore, active void works after " + session.getVoidAttempts() + "! Finishing.");
            break;
         }
      }

      // if we ended with a void product after all the attempts
      // we must set void status of the existent processed product to true
      if (currentProduct.isVoid()) {
         Logging.printLogDebug(logger, session, "Product still void. Finishing active void.");

      }

      return currentProduct;

   }

   /**
    * This method serializes the crawled sku instance and put its raw bytes on a kinesis stream. The instance passed as parameter is not altered. Instead we perform a clone to securely alter the
    * attributes.
    *
    * @param product data to send
    */
   private void sendToKinesis(Product product) {
      if (GlobalConfigurations.executionParameters.mustSendToKinesis() && (!product.isVoid() || session instanceof InsightsCrawlerSession || session instanceof ToBuyCrawlerSession)) {
         Product p = ProductDTO.convertProductToKinesisFormat(product, session);

         long productStartTime = System.currentTimeMillis();

         KPLProducer.getInstance().put(p, session);

         JSONObject kinesisProductFlowMetadata = new JSONObject().put("aws_elapsed_time", System.currentTimeMillis() - productStartTime)
            .put("aws_type", "kinesis")
            .put("kinesis_flow_type", "product");

         Logging.logInfo(logger, session, kinesisProductFlowMetadata, "AWS TIMING INFO");
      }
   }


   protected final <T> T cache(String key, int ttl, RequestMethod requestMethod, Request request, Function<Response, T> function) {
      String component = getClass().getSimpleName() + ":" + key;
      return cacheClient.update(component, request, requestMethod, session, dataFetcher, false, ttl, function);
   }

   protected final <T> T cache(String key, RequestMethod requestMethod, Request request, Function<Response, T> function) {
      String component = getClass().getSimpleName() + ":" + key;
      return cacheClient.update(component, request, requestMethod, session, dataFetcher, function);
   }

   protected final List<Cookie> cacheCookies(String key, RequestMethod requestMethod, Request request, Boolean force) {
      String component = getClass().getSimpleName() + ":" + key;
      return cacheClient.update(component, request, requestMethod, session, dataFetcher, force, Response::getCookies);
   }

   /**
    * This method is responsible for the main post processing stages of a crawled product. It takes care of the following tasks <br> 1. Print the crawled information; <br> 2. Persist the product; <br>
    * 3. Fetch the previous processed product. Which is a product with the same processed id as the current crawled product;</li> <br> 4. Create a new ProcessedModel; <br> 5. Persist the new
    * ProcessedModel;
    * <p>
    * In this method we also have the so called 'truco' stage. In cases that we already have the ProcessedModel, we will only update the informations of the previous ProcessedModel with the new
    * information crawled. But we don't update the information in the first try. When we detect some important change, such as in sku availability or price, we run the process all over again. The
    * crawler runs again and all the above enumerated stages are repeated, just to be shure that the information really changed or if it isn't a crawling bug or an URL blocking, by the ecommerce
    * website.
    * </p>
    * <p>
    * This process of rerun the crawler and so on, is repeated, until a maximum number of tries, or until we find two consecutive equals sets of crawled informations. If this occurs, then we persist
    * the new ProcessedModel. If the we run all the truco checks, and don't find consistent information, the crawler doesn't persist the new ProcessedModel.
    * </p>
    *
    * @param product
    */
   private void processProduct(Product product) {
      JSONObject productJson = DatabaseDataFetcher.fetchProductInElastic(product, session);
      Persistence.updateFrozenServerTask(product, productJson, ((SeedCrawlerSession) session));
   }


   private void printCrawledInformation(Product product) {

      try {
         String status = getFirstPartyRegexStatus(product);

         if (product.getAvailable() && status.equalsIgnoreCase("3P")) {
            Logging.printLogWarn(logger, session, "REGEX PROBLEM!");

            if (session instanceof TestCrawlerSession) {
               throw new MalformedProductException("THIS PRODUCT IS AVAILABLE BUT THIS MARKET REGEX DOES NOT MATCHES "
                  + "WITH NONE OF SELLERS NAMES IN THIS PRODUCT OFFERS " + getFirstPartyRegex(product));
            }
         }

         Logging.printLogInfo(logger, session, "Crawled information: " + "\nmarketId: " + session.getMarket().getNumber() + product.toString() +
            "\nregex_status: " + status);
      } catch (MalformedProductException e) {
         Exporter.collectError(e, session);
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
   }

   private String getFirstPartyRegexStatus(Product product) {
      String status = "-";

      Offers offers = product.getOffers();
      if (offers != null && !offers.isEmpty()) {
         status = "3P";

         for (Offer offer : offers.getOffersList()) {
            if (offer.getSlugSellerName().matches(session.getMarket().getFirstPartyRegex())) {
               status = "1P";
               break;
            }
         }
      }

      return status;
   }

   private String getFirstPartyRegex(Product product) {

      StringBuilder stringBuilder = new StringBuilder();
      Offers offers = product.getOffers();
      if (offers != null && !offers.isEmpty()) {
         for (Offer offer : offers.getOffersList()) {
            stringBuilder.append("\n[");
            stringBuilder.append(offer.getSlugSellerName());
            stringBuilder.append("]");

         }
      }
      return stringBuilder.toString();
   }


   @Override
   public void onFinish() {
      try {
         if (!(session instanceof TestCrawlerSession)) {
            S3Service.uploadCrawlerSessionContentToAmazon(session);
         }

         // close the webdriver
         if (webdriver != null) {
            Logging.printLogDebug(logger, session, "Terminating Chromium instance ...");
            webdriver.terminate();
         }

         List<SessionError> errors = session.getErrors();

         // errors collected manually
         // they can be exceptions or business logic errors
         // and are all gathered inside the session
         if (!errors.isEmpty()) {
            Logging.printLogWarn(logger, session, "Task failed [" + session.getOriginalURL() + "]");
            session.setTaskStatus(Task.STATUS_FAILED);
         } else {
            session.setTaskStatus(Task.STATUS_COMPLETED);
         }

         // only print statistics of void and truco if we are running an Insights session crawling
         if (session instanceof InsightsCrawlerSession) {
            Logging.printLogInfo(logger, session, "[ACTIVE_VOID_ATTEMPTS]" + session.getVoidAttempts());
         }

      } catch (Exception e) {
         Exporter.collectError(e, session);
         Logging.printLogWarn(logger, session, "Task failed [" + session.getOriginalURL() + "]");
         session.setTaskStatus(Task.STATUS_FAILED);
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      Logging.logInfo(logger, session, new JSONObject().put("elapsed_time", System.currentTimeMillis() - session.getStartTime()), "END");
   }

}
