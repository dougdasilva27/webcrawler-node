package br.com.lett.crawlernode.core.models;

import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.exceptions.RequestException;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

public class RequestConverter {

   protected static final Logger logger = LoggerFactory.getLogger(RequestConverter.class);

   private static final String BODY_QUEUE = "queue";
   private static final String BODY_CLASS_NAME = "className";
   private static final String BODY_OPTIONS = "options";
   private static final String BODY_INTERNAL_ID = "internalId";
   private static final String BODY_SUPPLIER_ID = "supplierId";
   private static final String BODY_MARKET = "market";
   private static final String BODY_PROCESSED_ID = "processedId" ;
   private static final String BODY_PARAMETERS = "parameters";
   private static final String BODY_SEED_TASK_ID ="taskId" ;
   private static final String BODY_RANKING_SCREENSHOT = "screenshot";
   private static final String MARKET_ID = "marketId";
   private static final String MARKET_NAME = "name";
   private static final String MARKET_FULL_NAME = "fullName";
   private static final String MARKET_CODE = "code";
   private static final String MARKET_REGEX= "regex";
   private static final String MARKET_USE_BROWSER= "use_browser";

   private RequestConverter() {
   }

   public static Request convert(HttpServletRequest req) {
      Request request;


      JSONObject body = JSONUtils.stringToJson(getRequestBody(req));

      String scraperType = body.optString("type");
      if (scraperType == null) {
         Logging.printLogError(logger, "Request is missing scraper type");
         throw new RequestException("scraperType not found");
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
      request.setQueueName(body.optString(BODY_QUEUE));
      request.setClassName(body.optString(BODY_CLASS_NAME));

      String options = body.optString(BODY_OPTIONS);
      if(options!=null && !options.isEmpty()) {
         request.setOptions(JSONUtils.stringToJson(options));
      }

      request.setInternalId(body.optString(BODY_INTERNAL_ID));

      String supplierIdString = body.optString(BODY_SUPPLIER_ID);

      if (supplierIdString != null && !supplierIdString.isEmpty()) {
            request.setSupplierId(Long.parseLong(supplierIdString.trim()));
      }


      JSONObject marketObj = body.getJSONObject(BODY_MARKET);
      if (marketObj != null) {
         request.setMarket(createMarket(marketObj));
      }


      String processedIdString = body.optString(BODY_PROCESSED_ID);
      if (processedIdString != null && !processedIdString.isEmpty()) {
         request.setProcessedId(Long.parseLong(processedIdString));
      }

      String parameters = body.optString(BODY_PARAMETERS);

      request.setParameter(parameters);
      request.setScraperType(scraperType);



      if (request instanceof CrawlerSeedRequest) {
         ((CrawlerSeedRequest) request).setTaskId(body.optString(BODY_SEED_TASK_ID));
      }

      if (request instanceof CrawlerRankingKeywordsRequest) {
         ((CrawlerRankingKeywordsRequest) request).setLocation(parameters);

         ((CrawlerRankingKeywordsRequest) request).setTakeAScreenshot(body.optBoolean(BODY_RANKING_SCREENSHOT,false));

      }

      return request;
   }

   private static Market createMarket(JSONObject marketObj) {
      return new Market(marketObj.optInt(MARKET_ID), marketObj.optString(MARKET_NAME), marketObj.optString(MARKET_FULL_NAME), marketObj.optString(MARKET_CODE),marketObj.optString(MARKET_REGEX),marketObj.optBoolean(MARKET_USE_BROWSER));
   }

   private static String getRequestBody(HttpServletRequest req) {
      try {
         return req.getReader().lines().collect(Collectors.joining(System.lineSeparator())).trim();
      } catch (IOException e) {
         logger.error("Failed to get body");
         throw new RequestException("Body");
      }
   }
}
