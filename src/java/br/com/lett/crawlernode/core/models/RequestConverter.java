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
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

public class RequestConverter {

   protected static final Logger logger = LoggerFactory.getLogger(RequestConverter.class);

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
      request.setQueueName(body.optString("queue"));
      request.setMessageId(body.optString("msgid"));

      request.setInternalId(body.optString("internalId"));

      String supplierIdString = body.optString("supplierId");

      if (supplierIdString != null) {
            request.setSupplierId(Long.parseLong(supplierIdString.trim()));
      }


      String marketIdString = (String) body.optQuery("market.id");
      if (marketIdString != null) {
         request.setMarketId(Integer.parseInt(marketIdString));
      }


      String processedIdString = body.optString("processedId");
      if (processedIdString != null) {
         request.setProcessedId(Long.parseLong(processedIdString));
      }

      String parameters = body.optString("parameters");

      request.setMessageBody(parameters);
      request.setScraperType(scraperType);



      if (request instanceof CrawlerSeedRequest) {
         ((CrawlerSeedRequest) request).setTaskId(body.optString("taskId"));
      }

      if (request instanceof CrawlerRankingKeywordsRequest) {
         ((CrawlerRankingKeywordsRequest) request).setLocation(parameters);

         ((CrawlerRankingKeywordsRequest) request).setTakeAScreenshot(body.optBoolean("screenshot",false));

      }

      return request;
   }

   private static String getRequestBody(HttpServletRequest req) {
      try (BufferedReader br = req.getReader()) {
         return br.readLine();
      } catch (IOException e) {
         logger.error("Failed to get body");
         throw new RequestException("Body");
      }
   }
}
