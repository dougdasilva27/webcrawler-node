package br.com.lett.crawlernode.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.crawler.DiscoveryCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.EqiCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.InsightsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.RatingReviewsCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.SeedCrawlerSession;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;

public class SessionFactory {

   private SessionFactory() {
   }

   private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);

   public static Session createSession(Request request, Market market) {
      String scraperType = request.getScraperType();

      if (ScrapersTypes.CORE.toString().equals(scraperType)) {
         return new InsightsCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.SEED.toString())) {
         return new SeedCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.DISCOVERER.toString())) {
         return new DiscoveryCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.RATING.toString())) {
         return new RatingReviewsCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.IMAGES_DOWNLOAD.toString())) {
         return new ImageCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.RANKING_BY_KEYWORDS.toString())) {
         return new RankingKeywordsSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.DISCOVERER_BY_KEYWORDS.toString())) {
         return new RankingDiscoverKeywordsSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.EQI.toString())) {
         return new EqiCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.EQI_DISCOVERER.toString())) {
         return new EqiRankingDiscoverKeywordsSession(request, scraperType, market);
      } else {
         Logging.printLogDebug(logger, "Scraper type not recognized." + "[" + scraperType + "]");
         return null;
      }
   }

   public static Session createTestSession(String url, Market market) {
      return new TestCrawlerSession(url, market);
   }

   public static Session createTestRankingKeywordsSession(String keyword, Market market) {
      return new TestRankingKeywordsSession(market, keyword);
   }
}
