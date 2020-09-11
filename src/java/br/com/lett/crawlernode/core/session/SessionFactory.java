package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.crawler.*;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionFactory {

   private static final Logger logger = LoggerFactory.getLogger(SessionFactory.class);

   public static Session createSession(Request request, Markets markets) {
      String scraperType = request.getScraperType();

      if (ScrapersTypes.CORE.toString().equals(scraperType)) {
         return new InsightsCrawlerSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.SEED.toString())) {
         return new SeedCrawlerSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.DISCOVERER.toString())) {
         return new DiscoveryCrawlerSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.RATING.toString())) {
         return new RatingReviewsCrawlerSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.IMAGES_DOWNLOAD.toString())) {
         return new ImageCrawlerSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.RANKING_BY_KEYWORDS.toString())) {
         return new RankingKeywordsSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.DISCOVERER_BY_KEYWORDS.toString())) {
         return new RankingDiscoverKeywordsSession(request, scraperType, markets);
      } else if (scraperType.equals(ScrapersTypes.EQI.toString())) {
         return new EqiCrawlerSession(request, scraperType, markets);
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
