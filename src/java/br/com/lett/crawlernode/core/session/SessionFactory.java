package br.com.lett.crawlernode.core.session;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.crawler.*;
import br.com.lett.crawlernode.core.session.ranking.EqiRankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingDiscoverKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.RankingKeywordsSession;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import enums.ScrapersTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      } else if (scraperType.equals(ScrapersTypes.TO_BUY.toString())) {
         return new ToBuyCrawlerSession(request, scraperType, market);
      } else if (scraperType.equals(ScrapersTypes.SENTINEL.toString())) {
         return new SentinelCrawlerSession(request, scraperType, market);
      } else {
         Logging.printLogDebug(logger, "Scraper type not recognized." + "[" + scraperType + "]");
         return null;
      }
   }

   public static Session createTestSession(String url, Market market, ScraperInformation scraperInformation, String fileMiranha) {
      return new TestCrawlerSession(url, market, scraperInformation, fileMiranha);
   }

   public static Session createTestRankingKeywordsSession(String keyword, Market market, ScraperInformation scraperInformation) {
      return new TestRankingKeywordsSession(market, keyword, scraperInformation);
   }
}
