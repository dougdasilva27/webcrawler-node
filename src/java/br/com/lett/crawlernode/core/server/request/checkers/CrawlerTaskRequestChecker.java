package br.com.lett.crawlernode.core.server.request.checkers;

import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.exceptions.SeedCrawlerSessionException;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerTaskRequestChecker {

   private CrawlerTaskRequestChecker() {
   }

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskRequestChecker.class);

   public static boolean checkRequest(Request request) {
      String scraperType = request.getScraperType();

      if (request instanceof CrawlerSeedRequest){
         try {
            return checkSeedTaskRequest(request);
         } catch (SeedCrawlerSessionException e) {
            e.printStackTrace();
         }
      }

      if (request instanceof ImageCrawlerRequest) {
         return checkImageTaskRequest(request);
      }

      if (ScrapersTypes.CORE.toString().equals(scraperType)) {
         if (request.getProcessedId() == null) {
            Logging.printLogError(logger, "Request is missing processedId");
            return false;
         }
         if (request.getInternalId() == null) {
            Logging.printLogError(logger, "Request is missing internalId");
            return false;
         }
      }

      if (ScrapersTypes.RANKING_BY_KEYWORDS.toString().equals(scraperType) && ((CrawlerRankingKeywordsRequest) request).getLocation() == null && ((CrawlerRankingKeywordsRequest) request).getLocationId() == null) {
         Logging.printLogError(logger, "Request is missing keyword");
         return false;
      }

      return true;
   }

   private static boolean checkImageTaskRequest(Request request) {
      ImageCrawlerRequest imageCrawlerRequest = (ImageCrawlerRequest) request;

      if (imageCrawlerRequest.getImageType() == null) {
         Logging.printLogError(logger, "Request is missing image type");
         return false;
      }
      if (imageCrawlerRequest.getProcessedId() == null) {
         Logging.printLogError(logger, "Request is missing processedId");
         return false;
      }
      if (imageCrawlerRequest.getInternalId() == null) {
         Logging.printLogError(logger, "Request is missing internalId");
         return false;
      }
      if (imageCrawlerRequest.getImageNumber() == null) {
         Logging.printLogError(logger, "Request is missing image number");
         return false;
      }

      return true;
   }

   private static boolean checkSeedTaskRequest(Request request) throws SeedCrawlerSessionException {
      if (request.isUseBrowser()) {
         throw new SeedCrawlerSessionException("This market doesn't work in Seed - Request in seed doesn't accept mode webdriver");
      }

      return false;
   }

}
