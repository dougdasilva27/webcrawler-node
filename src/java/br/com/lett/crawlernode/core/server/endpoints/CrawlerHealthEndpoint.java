package br.com.lett.crawlernode.core.server.endpoints;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.net.httpserver.HttpExchange;
import br.com.lett.crawlernode.core.server.ServerCrawler;
import br.com.lett.crawlernode.util.Logging;

public class CrawlerHealthEndpoint {

  protected static final Logger logger = LoggerFactory.getLogger(CrawlerHealthEndpoint.class);

  public static String perform() throws IOException {
    String response = ServerCrawler.MSG_SERVER_HEALTH_OK;


    Logging.printLogDebug(logger, "Health: " + ServerCrawler.MSG_SERVER_HEALTH_OK);

    return response;
  }

}
