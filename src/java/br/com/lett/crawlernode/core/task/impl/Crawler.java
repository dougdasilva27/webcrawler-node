package br.com.lett.crawlernode.core.task.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.task.Scheduler;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.config.CrawlerConfig;
import br.com.lett.crawlernode.database.DBSlack;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.database.PersistenceResult;
import br.com.lett.crawlernode.database.ProcessedModelPersistenceResult;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.processor.Processor;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateConstants;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.TestHtmlBuilder;
import containers.ProcessedComparison;
import models.Processed;
import models.prices.Prices;

/**
 * The Crawler superclass. All crawler tasks must extend this class to override both the shouldVisit
 * and extract methods.
 * 
 * @author Samir Leao
 *
 */

public class Crawler extends Task {

  protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);

  protected static final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|ico|tiff?|mid|mp2|mp3|mp4"
      + "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))(\\?.*)?$");

  /**
   * Maximum attempts during active void analysis It's essentially the number of times that we will
   * rerun the extract method to crawl a product from a page
   */
  protected static final int MAX_VOID_ATTEMPTS = 3;

  protected static final int MAX_TRUCO_ATTEMPTS = 3;

  protected CrawlerConfig config;

  protected CrawlerWebdriver webdriver;

  /**
   * Cookies that must be used to fetch the sku page this attribute is set by the
   * handleCookiesBeforeFetch method.
   */
  protected List<Cookie> cookies;


  public Crawler(Session session) {
    this.session = session;
    this.cookies = new ArrayList<>();

    createDefaultConfig();
  }

  /**
   * Create the config with default values
   */
  private void createDefaultConfig() {
    this.config = new CrawlerConfig();
    this.config.setFetcher(Fetcher.STATIC);
    this.config.setProxyList(new ArrayList<String>());
    this.config.setConnectionAttempts(0);
  }

  /**
   * Overrides the run method that will perform a task within a thread. The actual thread performs
   * it's computation controlled by an Executor, from Java's Executors Framework.
   */
  @Override
  public void processTask() {
    try {
      if (session instanceof TestCrawlerSession) {
        testRun();
      } else {
        productionRun();
      }
    } catch (Exception e) {
      DBSlack.reportCrawlerErrors(session, CommonMethods.getStackTrace(e));
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }
  }

  @Override
  public void onStart() {
    Logging.printLogDebug(logger, session, "START");
  }

  @Override
  public void onFinish() {

    try {
      // Logging.printLogDebug(logger, session, "Running crawler onFinish() method...");

      // close the webdriver
      if (webdriver != null) {
        Logging.printLogDebug(logger, session, "Terminating PhantomJS instance ...");
        webdriver.terminate();
      }

      List<SessionError> errors = session.getErrors();

      // errors collected manually
      // they can be exceptions or business logic errors
      // and are all gathered inside the session
      if (!errors.isEmpty()) {
        Logging.printLogError(logger, session, "Task failed [" + session.getOriginalURL() + "]");

        Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_FAILED, session, GlobalConfigurations.dbManager.connectionPanel);

        session.setTaskStatus(Task.STATUS_FAILED);
      }

      // only remove the task from queue if it was flawless
      // and if we are not testing, because when testing there is no message processing
      else if (session instanceof InsightsCrawlerSession || session instanceof SeedCrawlerSession || session instanceof DiscoveryCrawlerSession) {
        Logging.printLogDebug(logger, session, "Task completed.");

        Persistence.setTaskStatusOnMongo(Persistence.MONGO_TASK_STATUS_DONE, session, GlobalConfigurations.dbManager.connectionPanel);

        session.setTaskStatus(Task.STATUS_COMPLETED);
      }

      // only print statistics of void and truco if we are running an Insights session crawling
      if (session instanceof InsightsCrawlerSession) {
        Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPTS]" + session.getVoidAttempts());
        Logging.printLogDebug(logger, session, "[TRUCO_ATTEMPTS]" + session.getTrucoAttempts());
      }


      Logging.logDebug(logger, session, new JSONObject().put("elapsed_time", System.currentTimeMillis() - session.getStartTime()), "END");

    } catch (Exception e) {
      DBSlack.reportCrawlerErrors(session, CommonMethods.getStackTrace(e));
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }
  }

  private void productionRun() {
    // if (session instanceof InsightsCrawlerSession) {
    // Logging.printLogDebug(logger, session, "Max attempts for request in this market: " +
    // session.getMaxConnectionAttemptsCrawler());
    // } else if (session instanceof ImageCrawlerSession) {
    // Logging.printLogDebug(logger, session, "Max attempts for request in this market: " +
    // session.getMaxConnectionAttemptsImages());
    // }

    // crawl informations and create a list of products
    List<Product> products = null;
    try {
      products = extract();
    } catch (Exception e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      products = new ArrayList<>();
    }

    Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

    // print crawled information
    for (Product product : products) {
      printCrawledInformation(product);
    }

    // insights session
    // there is only one product that will be selected
    // by it's internalId, passed by the crawler session
    if (session instanceof InsightsCrawlerSession) {

      // get crawled product by it's internalId
      Logging.printLogDebug(logger, session, "Selecting product with internalId " + ((InsightsCrawlerSession) session).getInternalId());
      Product crawledProduct = filter(products, ((InsightsCrawlerSession) session).getInternalId());

      // if the product is void run the active void analysis
      Product activeVoidResultProduct = crawledProduct;
      if (crawledProduct.isVoid()) {
        Logging.printLogDebug(logger, session, "Product is void...going to start the active void.");
        try {
          activeVoidResultProduct = activeVoid(crawledProduct);
        } catch (Exception e) {
          Logging.printLogError(logger, session, "Error in active void method.");
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
          SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
          session.registerError(error);
        }
      }

      // after active void analysis we have the resultant
      // product after the extra extraction attempts
      // if the resultant product is not void, the we will process it
      if (!activeVoidResultProduct.isVoid()) {
        try {
          processProduct(activeVoidResultProduct);
        } catch (Exception e) {
          Logging.printLogError(logger, session, "Error in process product method.");
          Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));

          SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
          session.registerError(error);
        }
      }

    }

    // discovery session
    // when processing a task of a suggested URL by the webcrawler or
    // an URL scheduled manually, we won't run active void and
    // we must process each crawled product
    else if (session instanceof DiscoveryCrawlerSession || session instanceof SeedCrawlerSession) {
      Logging.printLogDebug(logger, session, "Processing session of type: " + session.getClass().getName());

      for (Product product : products) {
        try {
          processProduct(product);
        } catch (Exception e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
          SessionError error = new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTrace(e));
          session.registerError(error);
        }
      }
    }
  }

  /**
   * Run method to be used when testing
   */
  private void testRun() {

    // crawl informations and create a list of products
    List<Product> products = null;
    try {
      products = extract();
    } catch (Exception e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      products = new ArrayList<>();
    }

    Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());

    for (Product p : products) {
      if (Test.pathWrite != null) {
        TestHtmlBuilder.buildProductHtml(p.toJSON(), Test.pathWrite, session);
      }

      printCrawledInformation(p);
    }
  }

  /**
   * This method is responsible for the main post processing stages of a crawled product. It takes
   * care of the following tasks <br>
   * 1. Print the crawled information; <br>
   * 2. Persist the product; <br>
   * 3. Fetch the previous processed product. Which is a product with the same processed id as the
   * current crawled product;</li> <br>
   * 4. Create a new ProcessedModel; <br>
   * 5. Persist the new ProcessedModel;
   * <p>
   * In this method we also have the so called 'truco' stage. In cases that we already have the
   * ProcessedModel, we will only update the informations of the previous ProcessedModel with the new
   * information crawled. But we don't update the information in the first try. When we detect some
   * important change, such as in sku availability or price, we run the process all over again. The
   * crawler runs again and all the above enumerated stages are repeated, just to be shure that the
   * information really changed or if it isn't a crawling bug or an URL blocking, by the ecommerce
   * website.
   * </p>
   * <p>
   * This process of rerun the crawler and so on, is repeated, until a maximum number of tries, or
   * until we find two consecutive equals sets of crawled informations. If this occurs, then we
   * persist the new ProcessedModel. If the we run all the truco checks, and don't find consistent
   * information, the crawler doesn't persist the new ProcessedModel.
   * </p>
   * 
   * @param product
   */
  private void processProduct(Product product) throws Exception {
    boolean mustEnterTrucoMode = false;

    // persist the product
    Persistence.persistProduct(product, session);

    // fetch the previous processed product stored on database
    Processed previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);

    if ((previousProcessedProduct == null && (session instanceof DiscoveryCrawlerSession || session instanceof SeedCrawlerSession))
        || previousProcessedProduct != null) {

      // create the new processed product
      Processed newProcessedProduct =
          Processor.createProcessed(product, session, previousProcessedProduct, GlobalConfigurations.processorResultManager);

      // the product doesn't exists yet
      if (previousProcessedProduct == null) {

        // if a new processed product was created
        if (newProcessedProduct != null) {

          // persist the new created processed product
          PersistenceResult persistenceResult = Persistence.persistProcessedProduct(newProcessedProduct, session);
          processPersistenceResult(persistenceResult);
          scheduleImages(persistenceResult, newProcessedProduct);

          return;
        }

        // the new processed product is null. Indicates that it occurred some faulty information
        // crawled in the product
        // this isn't supposed to happen in insights mode, because previous to this process we ran
        // into
        // the active void analysis. This case will only happen in with discovery url or seed url,
        // where we probably doesn't
        // have the product on the database yet.
        else {
          // if we haven't a previous processed, and the new processed was null,
          // we don't have anything to give a trucada!
          Logging.printLogDebug(logger, session,
              "New processed product is null, and don't have a previous processed. Exiting processProduct method...");
          return;
        }
      }


      else { // we already have a processed product, so we must decide if we update

        if (newProcessedProduct != null) {

          // the two processed are different, so we must enter in truco mode
          if (compare(previousProcessedProduct, newProcessedProduct)) {
            mustEnterTrucoMode = true;
            Logging.printLogDebug(logger, session, "Must enter in truco mode.");
          }

          // the two processed are equals, so we can update it
          else {

            // get the id of the processed product on database
            // if it was only updated it will be the id of the previous existent processed product
            // if a new processed was created, it will be the id generated by the
            // this id will be added to the found_products field on the task document on Mongo
            PersistenceResult persistenceResult = Persistence.persistProcessedProduct(newProcessedProduct, session);
            processPersistenceResult(persistenceResult);
            scheduleImages(persistenceResult, newProcessedProduct);
            return;
          }
        }

      }

      // truco!
      if (mustEnterTrucoMode) {
        Logging.printLogDebug(logger, session, "Entering truco mode...");
        truco(newProcessedProduct, previousProcessedProduct);
      }
    }
  }

  /**
   * 
   * @param persistenceResult
   */
  private void processPersistenceResult(PersistenceResult persistenceResult) {
    Long createdId = null;
    Long modifiedId = null;
    if (persistenceResult instanceof ProcessedModelPersistenceResult) {
      createdId = ((ProcessedModelPersistenceResult) persistenceResult).getCreatedId();
      modifiedId = ((ProcessedModelPersistenceResult) persistenceResult).getModifiedId();
    }

    if (createdId != null) {
      Persistence.appendProcessedIdOnMongo(createdId, session, GlobalConfigurations.dbManager.connectionPanel);
      Persistence.appendCreatedProcessedIdOnMongo(createdId, session, GlobalConfigurations.dbManager.connectionPanel);
    } else if (modifiedId != null) {
      Persistence.appendProcessedIdOnMongo(modifiedId, session, GlobalConfigurations.dbManager.connectionPanel);
    }
  }


  private void scheduleImages(PersistenceResult persistenceResult, Processed processed) {
    Long createdId = null;
    if (persistenceResult instanceof ProcessedModelPersistenceResult) {
      createdId = ((ProcessedModelPersistenceResult) persistenceResult).getCreatedId();
    }

    if (createdId != null) {
      Logging.printLogDebug(logger, session, "Scheduling images download tasks...");
      Scheduler.scheduleImages(session, Main.queueHandler, processed, createdId);
    }

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
   * Performs the data extraction of the URL in the session. The four main steps of this method are:
   * <ul>
   * <li>Handle Cookies: Set any necessary cookies to do a proper http request of the page.</li>
   * <li>Handle URL: Makes any necessary modification on the URL in the session, before the
   * request.</li>
   * <li>Fetch: Do a http request and fetch the page data as a DOM.</li>
   * <li>Extraction: Crawl all skus in the URL on the crawling session.</li>
   * </ul>
   * 
   * @return An array with all the products crawled in the URL passed by the CrawlerSession, or an
   *         empty array list if no product was found.
   */
  public List<Product> extract() throws Exception {

    // in cases we are running a truco iteration
    if (webdriver != null) {
      webdriver.terminate();
    }

    // handle cookie
    if (cookies.isEmpty()) {
      handleCookiesBeforeFetch();
    }

    // handle URL modifications
    String url = handleURLBeforeFetch(session.getOriginalURL());
    session.setOriginalURL(url);

    Object obj = fetch();
    session.setProductPageResponse(obj);
    List<Product> products = new ArrayList<>();

    try {
      if (obj instanceof Document) {
        products = extractInformation((Document) obj);
      } else if (obj instanceof JSONObject) {
        products = extractInformation((JSONObject) obj);
      } else if (obj instanceof JSONArray) {
        products = extractInformation((JSONArray) obj);
      }
    } catch (Exception e) {
      Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
    }

    return products;
  }


  /**
   * Contains all the logic to sku information extraction. Must be implemented on subclasses.
   * 
   * @param Document
   * @return A product with all it's crawled informations
   */
  public List<Product> extractInformation(Document document) throws Exception {
    return new ArrayList<>();
  }

  /**
   * Contains all the logic to sku information extraction. Must be implemented on subclasses.
   * 
   * @param JSONObject
   * @return A product with all it's crawled informations
   */
  public List<Product> extractInformation(JSONObject json) throws Exception {
    return new ArrayList<>();
  }

  /**
   * Contains all the logic to sku information extraction. Must be implemented on subclasses.
   * 
   * @param JSONArray
   * @return A product with all it's crawled informations
   */
  public List<Product> extractInformation(JSONArray array) throws Exception {
    return new ArrayList<>();
  }

  /**
   * Request the sku URL and parse to a DOM format. This method uses the preferred fetcher according
   * to the crawler configuration. If the fetcher is static, then we use de StaticDataFetcher,
   * otherwise we use the DynamicDataFetcher.
   * 
   * Subclasses can override this method for crawl another apis and pages. In Princesadonorte the
   * product page has nothing, but we need the url for crawl this market api.
   * 
   * Return only {@link Document}
   * 
   * @return Parsed HTML in form of a Document.
   */
  protected Object fetch() {
    String html = "";
    if (config.getFetcher() == Fetcher.STATIC) {
      html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, session.getOriginalURL(), null, cookies);
    } else {
      webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

      if (webdriver != null) {
        html = webdriver.getCurrentPageSource();
      }
    }

    return Jsoup.parse(html);
  }

  /**
   * Compare ProcessedModel p1 against p2 p2 is suposed to be the truco, that is the model we are
   * checking against Obs: this method expects that both p1 and p2 are different from null. According
   * to the method processProduct, this never occurs when the compare(p1, p2) method is called.
   * 
   * @param p1
   * @param p2
   * @return true if they are different or false otherwise
   */
  private boolean compare(Processed p1, Processed p2) {
    ProcessedComparison comparison = p1.compareHugeChanges(p2);

    if (comparison.changed) {
      Logging.printLogInfo(logger, session, "Change detected on ProcessedModel [field " + comparison.field + "]");
      Logging.printLogDebug(logger, session, "Went from " + comparison.from + " to " + comparison.to);
    }

    return comparison.changed;
  }

  private void printCrawledInformation(Product product) {
    Logging.printLogDebug(logger, session, "Crawled information: " + "\nmarketId: " + session.getMarket().getNumber() + product.toString());
  }

  /**
   * Get only the product with the desired internalId.
   * 
   * @param products
   * @param internalId
   * @return The product with the desired internal id, or an empty product if it was not found.
   */
  private Product filter(List<Product> products, String desiredInternalId) {
    Logging.printLogDebug(logger, session, "Desired internalId " + desiredInternalId);
    for (Product product : products) {
      String crawledInternalId = product.getInternalId();
      if (crawledInternalId != null && crawledInternalId.equals(desiredInternalId)) {
        return product;
      }
    }

    Logging.printLogDebug(logger, session, "Product with internalId " + desiredInternalId + " not found.");
    return new Product();
  }

  /**
   * This method performs an active analysis of the void status.
   * 
   * @param product the crawled product
   * @return The resultant product from the analysis
   */
  private Product activeVoid(Product product) throws Exception {
    String nowISO = new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");

    // fetch the previous processed product
    // if a processed already exists and is void, then
    // we won't perform new attempts to extract the current product
    Processed previousProcessedProduct = Processor.fetchPreviousProcessed(product, session);
    if (previousProcessedProduct != null && previousProcessedProduct.isVoid()) {
      Logging.printLogDebug(logger, session, "The previous processed is void. Returning...");

      Logging.printLogDebug(logger, session, "Updating LRT ...");
      Persistence.updateProcessedLRT(nowISO, session);

      Logging.printLogDebug(logger, session, "Updating behavior of processedId: " + previousProcessedProduct.getId());
      new Processor().updateBehaviorTest(previousProcessedProduct, nowISO, null, false, "void", null, new Prices(), null, session);
      Persistence.updateProcessedBehaviour(previousProcessedProduct.getBehaviour(), session, previousProcessedProduct.getId());

      return product;
    }

    Logging.printLogDebug(logger, session, "Starting active void attempts...");

    // starting the active void iterations
    // until a maximum number of attempts, we will rerun the extract
    // method and check if the newly extracted product is void
    // in case it isn't, the loop interrupts and returns the product
    // when attempts reach it's maximum, we interrupt the loop and return the last extracted
    // product, even if it's void
    Product currentProduct = product;
    while (true) {
      session.incrementVoidAttemptsCounter();

      Logging.printLogDebug(logger, session, "[ACTIVE_VOID_ATTEMPT]" + session.getVoidAttempts());
      List<Product> products = extract();
      Logging.printLogDebug(logger, session, "Number of crawled products: " + products.size());
      currentProduct = filter(products, ((InsightsCrawlerSession) session).getInternalId());

      if (session.getVoidAttempts() >= MAX_VOID_ATTEMPTS || !currentProduct.isVoid()) {
        break;
      }
    }

    // if we ended with a void product after all the attempts
    // we must set void status of the existent processed product to true
    if (currentProduct.isVoid()) {
      Logging.printLogDebug(logger, session, "Product is void.");

      // set previous processed as void
      if (previousProcessedProduct != null && !previousProcessedProduct.isVoid()) {
        Logging.printLogDebug(logger, session, "Setting previous processed void status to true...");
        Persistence.setProcessedVoidTrue(session);

        Logging.printLogDebug(logger, session, "Updating LRT ...");
        Persistence.updateProcessedLRT(nowISO, session);

        Logging.printLogDebug(logger, session, "Updating LMS ...");
        Persistence.updateProcessedLMS(nowISO, session);

        Logging.printLogDebug(logger, session, "Updating behavior of processedId: " + previousProcessedProduct.getId());
        new Processor().updateBehaviorTest(previousProcessedProduct, nowISO, null, false, "void", null, new Prices(), null, session);
        Persistence.updateProcessedBehaviour(previousProcessedProduct.getBehaviour(), session, previousProcessedProduct.getId());
      }
    }

    return currentProduct;

  }

  /**
   * Run the 'truco' attempts. Before entering in this method, the two parameters newProcessed and
   * previousProcessed where compared.
   * 
   * @param newProcessed the initial processed model, from which we will start the iteration
   * @param previousProcessed the processed model that already exists in database
   */
  private void truco(Processed newProcessed, Processed previousProcessed) throws Exception {
    Processed currentTruco = newProcessed;
    Processed next;

    String nowISO = new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");

    while (true) {
      session.incrementTrucoAttemptsCounter();

      List<Product> products = extract();

      /*
       * when we are processing all the the products in array (mode discovery) we will select only the
       * product being 'trucado'
       */
      Product localProduct = filter(products, currentTruco.getInternalId());

      // proceed the iteration only if the product is not void
      if (localProduct != null && !localProduct.isVoid()) {
        Persistence.persistProduct(localProduct, session);

        next = Processor.createProcessed(localProduct, session, previousProcessed, GlobalConfigurations.processorResultManager);

        if (next != null) {
          if (compare(next, currentTruco)) {
            currentTruco = next;
          }

          // we found two consecutive equals processed products, persist and end
          else {
            Persistence.insertProcessedIdOnMongo(session, GlobalConfigurations.dbManager.connectionPanel);

            PersistenceResult persistenceResult = Persistence.persistProcessedProduct(next, session);
            Persistence.updateProcessedLMT(nowISO, session);
            processPersistenceResult(persistenceResult);
            scheduleImages(persistenceResult, next);

            JSONObject changes = next.getChanges();
            // take a screenshot if price was change
            if (changes != null && changes.has("price")) {
              // URLBox.takeAScreenShot(next.getUrl(), session, 1, null);
            }

            return;
          }
        }
      }

      if (session.getTrucoAttempts() >= MAX_TRUCO_ATTEMPTS) {
        Logging.printLogDebug(logger, session, "Ended truco session but will not persist the product.");

        // register business logic error on session
        SessionError error = new SessionError(SessionError.BUSINESS_LOGIC, "Ended truco session but will not persist the product.");
        session.registerError(error);

        break;
      }
    }
  }

}

