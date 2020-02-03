package br.com.lett.crawlernode.core.server;

import br.com.lett.crawlernode.core.server.endpoints.CrawlerHealthEndpoint;
import br.com.lett.crawlernode.core.server.endpoints.CrawlerTaskEndpoint;
import br.com.lett.crawlernode.core.server.request.*;
import br.com.lett.crawlernode.core.server.request.checkers.CrawlerTaskRequestChecker;
import br.com.lett.crawlernode.util.Logging;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import enums.ScrapersTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ServerHandler implements HttpHandler {

  protected static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

  public static final String POST = "POST";
  public static final String GET = "GET";

  private static final String MSG_ATTR_HEADER_PREFIX = "X-aws-sqsd-attr-";

  private static final String MSG_ATTR_TASK_ID = "taskId";
  private static final String MSG_ATTR_MARKET_ID = "marketId";
  private static final String MSG_ATTR_PROCESSED_ID = "processedId";
  private static final String MSG_ATTR_INTERNAL_ID = "internalId";
  private static final String MSG_ATTR_IMG_NUMBER = "number";
  private static final String MSG_ATTR_IMG_TYPE = "type";
  private static final String MSG_ATTR_CATEGORY_ID = "categoryId";
  private static final String MSG_ATTR_SCREENSHOT = "screenshot";
  private static final String MSG_ATTR_SCRAPER_TYPE = "scraperType";

  private static final String MSG_ID_HEADER = "X-aws-sqsd-msgid";
  private static final String SQS_NAME_HEADER = "X-aws-sqsd-queue";

  @Override
  public void handle(HttpExchange t) throws IOException {
    String endpoint = t.getHttpContext().getPath();
    String response;

    // handle request on the task endpoint
    if (Server.ENDPOINT_TASK.equals(endpoint)) {
      Logging.printLogInfo(logger, "Received a request on " + Server.ENDPOINT_TASK);

      Request request = parseRequest(t);

      Logging.printLogDebug(logger, request.toString());

      if (CrawlerTaskRequestChecker.checkRequestMethod(request)) {
        if (CrawlerTaskRequestChecker.checkRequest(request)) {
          response = CrawlerTaskEndpoint.perform(t, request);
        } else {
          response = Server.MSG_BAD_REQUEST;
          t.sendResponseHeaders(Server.HTTP_STATUS_CODE_BAD_REQUEST, response.length());
        }
      } else {
        response = Server.MSG_METHOD_NOT_ALLOWED;
        t.sendResponseHeaders(Server.HTTP_STATUS_CODE_METHOD_NOT_ALLOWED, response.length());
      }
    }

    // handle request on the health check endpoint
    else if (Server.ENDPOINT_HEALTH_CHECK.equals(endpoint)) {
      Logging.printLogDebug(logger, "Received a request on " + Server.ENDPOINT_HEALTH_CHECK);

      response = CrawlerHealthEndpoint.perform(t);
    }

    // handle request on the test endpoint
    else {
      Logging.printLogDebug(logger, "Received a request on " + Server.ENDPOINT_TEST);

      response = "Testing crawler...";
    }

    OutputStream os = t.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  private Request parseRequest(HttpExchange t) throws IOException {
    Headers headers = t.getRequestHeaders();
    String scraperType = headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCRAPER_TYPE);
    Request request;

    if (ScrapersTypes.IMAGES_DOWNLOAD.toString().equals(scraperType)) {
      request = new ImageCrawlerRequest();
    } else if (ScrapersTypes.RANKING_BY_KEYWORDS.toString().equals(scraperType)
        || ScrapersTypes.DISCOVERER_BY_KEYWORDS.toString().equals(scraperType)) {
      request = new CrawlerRankingKeywordsRequest();
    } else if (ScrapersTypes.RANKING_BY_CATEGORIES.toString().equals(scraperType)
        || ScrapersTypes.DISCOVERER_BY_CATEGORIES.toString().equals(scraperType)) {
      request = new CrawlerRankingCategoriesRequest();
    } else if (ScrapersTypes.SEED.toString().equals(scraperType)) {
      request = new CrawlerSeedRequest();
    } else {
      request = new Request();
    }

    request.setRequestMethod(t.getRequestMethod().toUpperCase());
    request.setQueueName(headers.getFirst(SQS_NAME_HEADER));
    request.setMessageId(headers.getFirst(MSG_ID_HEADER));
    request.setInternalId(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_INTERNAL_ID));

    String marketIdString = headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_MARKET_ID);
    if (marketIdString != null) {
      request.setMarketId(Integer.parseInt(marketIdString));
    }

    String processedIdString = headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_PROCESSED_ID);
    if (processedIdString != null) {
      request.setProcessedId(Long.parseLong(processedIdString));
    }

    String body = getRequestBody(t.getRequestBody());

    request.setMessageBody(body);
    request.setScraperType(scraperType);

    if (request instanceof ImageCrawlerRequest) {
      ((ImageCrawlerRequest) request).setImageNumber(Integer.parseInt(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_IMG_NUMBER)));
      ((ImageCrawlerRequest) request).setImageType(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_IMG_TYPE));
    }

    if (request instanceof CrawlerSeedRequest) {
      ((CrawlerSeedRequest) request).setTaskId(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_TASK_ID));
    }

    if (request instanceof CrawlerRankingKeywordsRequest) {
      ((CrawlerRankingKeywordsRequest) request).setLocation(body);

      if (headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCREENSHOT) != null
          && Boolean.valueOf(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_SCREENSHOT))) {
        ((CrawlerRankingKeywordsRequest) request).setTakeAScreenshot(true);
      } else {
        ((CrawlerRankingKeywordsRequest) request).setTakeAScreenshot(false);
      }

    }

    if (request instanceof CrawlerRankingCategoriesRequest) {
      ((CrawlerRankingCategoriesRequest) request).setLocation(headers.getFirst(MSG_ATTR_HEADER_PREFIX + MSG_ATTR_CATEGORY_ID));
      ((CrawlerRankingCategoriesRequest) request).setCategoryUrl(body);
    }

    return request;
  }

  private String getRequestBody(InputStream t) throws IOException {
    InputStreamReader isr = new InputStreamReader(t, StandardCharsets.UTF_8);
    BufferedReader br = new BufferedReader(isr);

    int b;
    StringBuilder buf = new StringBuilder(512);
    while ((b = br.read()) != -1) {
      buf.append((char) b);
    }

    br.close();
    isr.close();

    return buf.toString();
  }
}
