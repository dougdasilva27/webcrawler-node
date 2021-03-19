package br.com.lett.crawlernode.core.server.endpoints;

import br.com.lett.crawlernode.core.server.ServerCrawler;
import br.com.lett.crawlernode.core.server.request.CrawlerRankingKeywordsRequest;
import br.com.lett.crawlernode.core.server.request.CrawlerSeedRequest;
import br.com.lett.crawlernode.core.server.request.ImageCrawlerRequest;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.server.request.checkers.CrawlerTaskRequestChecker;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
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

public class CrawlerTaskEndpoint extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskEndpoint.class);

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
      String response;
      Logging.printLogInfo(logger, "Received a request on " + ServerCrawler.ENDPOINT_TASK);

      String scraperType = req.getHeader(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCRAPER_TYPE);
      if (hasScraperType(scraperType)) {
         Request request = parseRequest(req, scraperType);

         if (CrawlerTaskRequestChecker.checkRequest(request)) {

            Logging.printLogDebug(logger, request.toString());

            response = perform(res, request);

         } else {
            response = ServerCrawler.MSG_BAD_REQUEST;
            res.setStatus(ServerCrawler.HTTP_STATUS_CODE_BAD_REQUEST);
         }
      } else {
         response = ServerCrawler.MSG_BAD_REQUEST;
      }
      res.getWriter().println(response);

   }


   private boolean hasScraperType(String scraperType) {
      if (scraperType != null) {
         for (ScrapersTypes type : ScrapersTypes.values()) {
            if (type.name().equalsIgnoreCase(scraperType)) {
               return true;
            }
         }
      } else {
         Logging.printLogError(logger, "Request is missing scraper type");
         return false;
      }

      Logging.printLogError(logger, "Scraper type not recognized." + "[" + scraperType + "]");
      return false;
   }

   private Request parseRequest(HttpServletRequest req, String scraperType) {
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

   public String getRequestBody(HttpServletRequest req) {
      try (BufferedReader br = req.getReader()) {
         return br.lines().collect(Collectors.joining(System.lineSeparator()));
      } catch (IOException e) {
         return "failed to get body!";
      }
   }

   public static String perform(HttpServletResponse res, Request request) {
      String response;

      Logging.printLogDebug(logger, "Creating session....");
      Session session = SessionFactory.createSession(request, GlobalConfigurations.markets);

      // create the task
      Logging.printLogDebug(logger, session, "Creating task for " + session.getOriginalURL());
      Task task = TaskFactory.createTask(session);

      // perform the task
      Logging.printLogDebug(logger, session, "Processing task ...");
      task.process();

      // check final task status
      if (Task.STATUS_COMPLETED.equals(session.getTaskStatus())) {
         response = ServerCrawler.MSG_TASK_COMPLETED;
         res.setStatus(ServerCrawler.HTTP_STATUS_CODE_OK);
         Logging.printLogDebug(logger, "TASK RESPONSE STATUS: " + ServerCrawler.HTTP_STATUS_CODE_OK);
         Main.server.incrementSucceededTasks();
      } else {
         response = ServerCrawler.MSG_TASK_FAILED;
         res.setStatus(ServerCrawler.HTTP_STATUS_CODE_SERVER_ERROR);
         Logging.printLogDebug(logger, "TASK RESPONSE STATUS: " + ServerCrawler.HTTP_STATUS_CODE_SERVER_ERROR);
         Main.server.incrementFailedTasks();
      }

      return response;
   }

}
