package br.com.lett.crawlernode.core.session.crawler;

import java.util.ArrayList;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionError;
import br.com.lett.crawlernode.core.session.TestType;
import enums.ScrapersTypes;

public class TestCrawlerSession extends Session {

  /** Number of truco checks */
  private int trucoAttemptsCounter;

  /** Number of readings to prevent a void status */
  private int voidAttemptsCounter;

  private TestType type;

  public TestCrawlerSession(String url, Market market) {
    super(market);

    // initialize counters
    this.trucoAttemptsCounter = 0;
    this.voidAttemptsCounter = 0;

    // creating the errors list
    this.crawlerSessionErrors = new ArrayList<SessionError>();

    // setting session id
    this.sessionId = "test";

    // setting Market
    this.market = market;

    // setting URL and originalURL
    this.originalURL = url;
  }


  public TestCrawlerSession(Request request, Market market) {
    super(market);

    if (ScrapersTypes.DISCOVERER.toString().equals(request.getScraperType())) {
      setType(TestType.DISCOVER);
    } else if (ScrapersTypes.IMAGES_DOWNLOAD.toString().equals(request.getScraperType())) {
      setType(TestType.IMAGE);
    } else if (ScrapersTypes.CORE.toString().equals(request.getScraperType())) {
      setType(TestType.INSIGHTS);
    } else if (ScrapersTypes.RATING.toString().equals(request.getScraperType())) {
      setType(TestType.RATING);
    } else {
      setType(TestType.SEED);
    }

    // initialize counters
    this.trucoAttemptsCounter = 0;
    this.voidAttemptsCounter = 0;

    // creating the errors list
    this.crawlerSessionErrors = new ArrayList<SessionError>();

    // setting session id
    this.sessionId = request.getMessageId();

    // setting Market
    this.market = market;

    // setting URL and originalURL
    this.originalURL = request.getMessageBody();
  }

  public int getTrucoAttempts() {
    return trucoAttemptsCounter;
  }

  public int getVoidAttempts() {
    return voidAttemptsCounter;
  }

  public void setVoidAttempts(int voidAttempts) {
    this.voidAttemptsCounter = voidAttempts;
  }

  public TestType getType() {
    return type;
  }


  public void setType(TestType type) {
    this.type = type;
  }


  public void incrementTrucoAttemptsCounter() {
    this.trucoAttemptsCounter++;
  }

  public void incrementVoidAttemptsCounter() {
    this.voidAttemptsCounter++;
  }

}
