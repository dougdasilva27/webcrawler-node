package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.RankingCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingCategoriesSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;

public class SessionFactory {

  private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);

  public static Session createSession(Request request, Markets markets) {
    String scraperType = request.getScraperType();

    if (ScrapersTypes.CORE.name().equals(scraperType)) {
      return new InsightsCrawlerSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.SEED.name())) {
      return new SeedCrawlerSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.DISCOVERER.name())) {
      return new DiscoveryCrawlerSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.RATING.name())) {
      return new RatingReviewsCrawlerSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.IMAGES_DOWNLOAD.name())) {
      return new ImageCrawlerSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.RANKING_BY_KEYWORDS.name())) {
      return new RankingKeywordsSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.DISCOVERER_BY_KEYWORDS.name())) {
      return new RankingDiscoverKeywordsSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.RANKING_BY_CATEGORIES.name())) {
      return new RankingCategoriesSession(request, scraperType, markets);
    } else if (scraperType.equals(ScrapersTypes.DISCOVERER_BY_CATEGORIES.name())) {
      return new RankingDiscoverCategoriesSession(request, scraperType, markets);
    } else {
      Logging.printLogDebug(logger, "Queue name not recognized." + "[" + scraperType + "]");
      return null;
    }
  }

  public static Session createTestSession(String url, Market market) {
    return new TestCrawlerSession(url, market);
  }

  public static Session createTestRankingKeywordsSession(String keyword, Market market) {
    return new TestRankingKeywordsSession(market, keyword);
  }

  public static Session createTestRankingCategoriesSession(String categorieUrl, Market market, String location) {
    return new TestRankingCategoriesSession(market, categorieUrl, location);
  }
}
