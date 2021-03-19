package br.com.lett.crawlernode.core.models;

import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.exceptions.RequestException;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestConverter {

   protected static final Logger logger = LoggerFactory.getLogger(RequestConverter.class);

   private static final String MSG_ATTR_TASK_ID = "taskId";
   private static final String MSG_ATTR_MARKET_ID = "marketId";
   private static final String MSG_ATTR_SUPPLIER_ID = "supplierId";
   private static final String MSG_ATTR_PROCESSED_ID = "processedId";
   private static final String MSG_ATTR_INTERNAL_ID = "internalId";
   private static final String MSG_ATTR_IMG_NUMBER = "number";
   private static final String MSG_ATTR_IMG_TYPE = "type";
   private static final String MSG_ATTR_SCREENSHOT = "screenshot";
   private static final String MSG_ID_HEADER = "X-aws-sqsd-msgid";
   private static final String SQS_NAME_HEADER = "X-aws-sqsd-queue";
   private static final String MSG_ATTR_HEADER_PREFIX = "X-aws-sqsd-attr-";
   private static final String MSG_ATTR_SCRAPER_TYPE = "scraperType";


   private RequestConverter() {
   }

   public static Request convert(HttpServletRequest req) {
      Request request;

      String scraperType = req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCRAPER_TYPE);
      if (scraperType == null) {
         Logging.printLogError(logger, "Request is missing scraper type");
         throw new RequestException(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCRAPER_TYPE);
      }

      if (ScrapersTypes.IMAGES_DOWNLOAD.toString().equals(scraperType)) {
         request = new ImageCrawlerRequest();
      } else if (ScrapersTypes.RANKING_BY_KEYWORDS.toString().equals(scraperType) || ScrapersTypes.DISCOVERER_BY_KEYWORDS.toString().equals(scraperType)
         || ScrapersTypes.EQI_DISCOVERER.toString().equals(scraperType)) {
         request = new CrawlerRankingKeywordsRequest();
      } else if (ScrapersTypes.SEED.toString().equals(scraperType)) {
         request = new CrawlerSeedRequest();
      } else {
         request = new Request();
      }

      request.setRequestMethod(req.getMethod());
      request.setQueueName(req.getHeader(SQS_NAME_HEADER));
      request.setMessageId(req.getHeader(MSG_ID_HEADER));
      request.setInternalId(req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_INTERNAL_ID));

      String supplierIdString = req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SUPPLIER_ID);

      if (supplierIdString != null) {
         request.setSupplierId(Long.parseLong(supplierIdString.trim()));
      }

      String marketIdString = req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_MARKET_ID);
      if (marketIdString != null) {
         request.setMarketId(Integer.parseInt(marketIdString));
      }

      String processedIdString = req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_PROCESSED_ID);
      if (processedIdString != null) {
         request.setProcessedId(Long.parseLong(processedIdString));
      }

      String body = getRequestBody(req);

      request.setMessageBody(body);
      request.setScraperType(scraperType);

      if (request instanceof ImageCrawlerRequest) {
         ((ImageCrawlerRequest) request).setImageNumber(Integer.parseInt(req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_IMG_NUMBER)));
         ((ImageCrawlerRequest) request).setImageType(req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_IMG_TYPE));
      }

      if (request instanceof CrawlerSeedRequest) {
         ((CrawlerSeedRequest) request).setTaskId(req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_TASK_ID));
      }

      if (request instanceof CrawlerRankingKeywordsRequest) {
         ((CrawlerRankingKeywordsRequest) request).setLocation(body);

         ((CrawlerRankingKeywordsRequest) request).setTakeAScreenshot(req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCREENSHOT) != null
            && Boolean.parseBoolean(req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCREENSHOT)));

      }

      return request;
   }

   private static String getRequestBody(HttpServletRequest req) {
      try (BufferedReader br = req.getReader()) {
         return br.lines().collect(Collectors.joining(System.lineSeparator()));
      } catch (IOException e) {
         logger.error("Failed to get body");
         throw new RequestException("Body");
      }
   }
}
