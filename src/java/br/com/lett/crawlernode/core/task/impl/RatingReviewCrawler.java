package br.com.lett.crawlernode.core.task.impl;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.CrawlerWebdriver;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.crawler.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.config.RatingCrawlerConfig;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class RatingReviewCrawler extends Task {

  protected static final Logger logger = LoggerFactory.getLogger(RatingReviewCrawler.class);

  /**
   * Cookies that must be used to fetch the sku page this attribute is set by the
   * handleCookiesBeforeFetch method.
   */
  protected List<Cookie> cookies;
  protected RatingCrawlerConfig config;
  protected CrawlerWebdriver webdriver;
  protected DataFetcher dataFetcher;


  public RatingReviewCrawler(Session session) {
    this.session = session;
    this.cookies = new ArrayList<>();

    createDefaultConfig();
  }

  private void createDefaultConfig() {
    this.config = new RatingCrawlerConfig();
    this.config.setFetcher(FetchMode.STATIC);
    this.config.setProxyList(new ArrayList<String>());
    this.config.setConnectionAttempts(0);
  }

  @Override
  public void processTask() {
    try {
      setDataFetcher();
      if (session instanceof RatingReviewsCrawlerSession) {
        runProduction();
      } else {
        runTest();
      }
    } catch (Exception e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }
  }

  @Override
  public void onStart() {
    Logging.printLogDebug(logger, session, "START");
  }

  @Override
  public void onFinish() {
    List<SessionError> errors = session.getErrors();

    // close the webdriver
    if (webdriver != null) {
      Logging.printLogDebug(logger, session, "Terminating PhantomJS instance ...");
      webdriver.terminate();
    }

    // errors collected manually
    // they can be exceptions or business logic errors
    // and are all gathered inside the session
    if (!errors.isEmpty()) {
      Logging.printLogWarn(logger, session, "Task failed [" + session.getOriginalURL() + "]");
      session.setTaskStatus(Task.STATUS_FAILED);
    }

    else {

      // only remove the task from queue if it was flawless
      // and if we are not testing, because when testing there is no message processing
      Logging.printLogDebug(logger, session, "Task completed.");

      session.setTaskStatus(Task.STATUS_COMPLETED);
    }

    Logging.printLogDebug(logger, session, "END");
  }

  private void setDataFetcher() {
    if (config.getFetcher() == FetchMode.STATIC) {
      dataFetcher = GlobalConfigurations.executionParameters.getUseFetcher() ? new ApacheDataFetcher() : new FetcherDataFetcher();
    } else if (config.getFetcher() == FetchMode.APACHE) {
      dataFetcher = new ApacheDataFetcher();
    } else if (config.getFetcher() == FetchMode.JAVANET) {
      dataFetcher = new JavanetDataFetcher();
    } else if (config.getFetcher() == FetchMode.FETCHER) {
      dataFetcher = new FetcherDataFetcher();
    }
  }

  public void runProduction() {
    if (cookies.isEmpty()) {
      handleCookiesBeforeFetch();
    }

    // apply URL modifications
    String modifiedURL = handleURLBeforeFetch(session.getOriginalURL());
    session.setOriginalURL(modifiedURL);

    Document document = fetch();
    try {
      RatingReviewsCollection ratingReviewsCollection = extractRatingAndReviews(document);

      if (session.getInternalId() != null) {
        // get only the desired rating and review, according to the internal id
        RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(session.getInternalId());

        if (ratingReviews != null) {
          printRatingsReviews(ratingReviews);
          Persistence.updateRating(ratingReviews, session.getInternalId(), session);

        } else {
          Logging.printLogWarn(logger, session, "Rating and reviews for internalId " + session.getInternalId() + " was not crawled.");
        }
      } else {
        for (RatingsReviews rating : ratingReviewsCollection.getRatingReviewsList()) {
          printRatingsReviews(rating);
          Persistence.updateRating(rating, rating.getInternalId(), session);
        }
      }

    } catch (Exception e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
      session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
    }
  }

  public void runTest() {
    if (cookies.isEmpty()) {
      handleCookiesBeforeFetch();
    }

    // apply URL modifications
    String modifiedURL = handleURLBeforeFetch(session.getOriginalURL());
    session.setOriginalURL(modifiedURL);

    Document document = fetch();
    try {
      RatingReviewsCollection ratingReviewsCollection = extractRatingAndReviews(document);

      for (RatingsReviews rating : ratingReviewsCollection.getRatingReviewsList()) {
        printRatingsReviews(rating);
      }

    } catch (Exception e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
      session.registerError(new SessionError(SessionError.EXCEPTION, CommonMethods.getStackTraceString(e)));
    }
  }

  private void sendToKinesis(RatingsReviews rating) {
    if (GlobalConfigurations.executionParameters.mustSendToKinesis()) {
      rating.setUrl(session.getOriginalURL());
      rating.setMarketId(session.getMarket().getNumber());
    }
  }

  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    /* subclasses must implement */
    return new RatingReviewsCollection();
  }

  protected void handleCookiesBeforeFetch() {
    /* subclasses must implement */
  }

  protected String handleURLBeforeFetch(String url) {
    /* subclasses must implement */
    return url;
  }

  private void printRatingsReviews(RatingsReviews ratingReviews) {
    Logging.printLogDebug(logger, session, "Internal Id: " + ratingReviews.getInternalId() + " " + ratingReviews.toString());
  }

  /**
   * Request the sku URL and parse to a DOM format.
   * 
   * @return Parsed HTML in form of a Document
   */
  protected Document fetch() {
    String html = "";
    if (config.getFetcher() == FetchMode.WEBDRIVER) {
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

      if (webdriver != null) {
        html = webdriver.getCurrentPageSource();
      }
    } else {
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(session.getOriginalURL()).build();
      Response response = dataFetcher.get(session, request);

      html = response.getBody();
    }

    return Jsoup.parse(html);
  }

}
