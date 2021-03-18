package br.com.lett.crawlernode.core.server;

import br.com.lett.crawlernode.core.server.endpoints.CrawlerHealthEndpoint;
import br.com.lett.crawlernode.core.server.endpoints.CrawlerTaskEndpoint;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.server.request.checkers.CrawlerTaskRequestChecker;
import br.com.lett.crawlernode.util.Logging;
import enums.ScrapersTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;


public class ServerHandler extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

   public static final String POST = "POST";
   public static final String GET = "GET";

   private static final String MSG_ATTR_HEADER_PREFIX = "X-aws-sqsd-attr-";

   private static final String MSG_ATTR_TASK_ID = "taskId";
   private static final String MSG_ATTR_MARKET_ID = "marketId";
   private static final String MSG_ATTR_SUPPLIER_ID = "supplierId";
   private static final String MSG_ATTR_PROCESSED_ID = "processedId";
   private static final String MSG_ATTR_INTERNAL_ID = "internalId";
   private static final String MSG_ATTR_IMG_NUMBER = "number";
   private static final String MSG_ATTR_IMG_TYPE = "type";
   private static final String MSG_ATTR_SCREENSHOT = "screenshot";
   private static final String MSG_ATTR_SCRAPER_TYPE = "scraperType";

   private static final String MSG_ID_HEADER = "X-aws-sqsd-msgid";
   private static final String SQS_NAME_HEADER = "X-aws-sqsd-queue";

   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
      String endpoint = req.getServletPath();
      String response;

      // handle request on the task endpoint
      if (ServerCrawler.ENDPOINT_TASK.equals(endpoint)) {
         Logging.printLogInfo(logger, "Received a request on " + ServerCrawler.ENDPOINT_TASK);

         Request request = parseRequest(req);

         Logging.printLogDebug(logger, request.toString());

         if (CrawlerTaskRequestChecker.checkRequestMethod(request)) {
            if (CrawlerTaskRequestChecker.checkRequest(request)) {
               response = CrawlerTaskEndpoint.perform(res, request);
            } else {
               response = ServerCrawler.MSG_BAD_REQUEST;
               res.setStatus(ServerCrawler.HTTP_STATUS_CODE_BAD_REQUEST);
            }
         } else {
            response = ServerCrawler.MSG_METHOD_NOT_ALLOWED;
            res.setStatus(ServerCrawler.HTTP_STATUS_CODE_METHOD_NOT_ALLOWED);
         }
      }

      // handle request on the health check endpoint
      else {
         if (ServerCrawler.ENDPOINT_HEALTH_CHECK.equals(endpoint)) {
            Logging.printLogDebug(logger, "Received a request on " + ServerCrawler.ENDPOINT_HEALTH_CHECK);

            response = CrawlerHealthEndpoint.perform();
         }

         // handle request on the test endpoint
         else {
            Logging.printLogDebug(logger, "Received a request on " + ServerCrawler.ENDPOINT_TEST);

            response = "Testing crawler...";
         }
      }
      res.getWriter().println(response);

   }

   private Request parseRequest(HttpServletRequest req) {
      String scraperType = req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCRAPER_TYPE);
      Request request;

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

   //esse método pega outras linhas
   protected String readBody(HttpServletRequest request) throws IOException {
      StringBuilder strBuilder = new StringBuilder();
      int bytesRead;
      char[] charBuffer = new char[128];
      try (BufferedReader bufferedReader = request.getReader()) {
         while ((bytesRead = bufferedReader.read(charBuffer, 0, 128)) > 0) {
            strBuilder.append(charBuffer, 0, bytesRead);
         }
      }
      return strBuilder.toString();
   }

   public String getRequestBody(HttpServletRequest req) {
      try (BufferedReader br = req.getReader()) {
         return br.lines().collect(Collectors.joining(System.lineSeparator()));
      } catch (IOException e) {
         return "failed";
      }
   }

}
