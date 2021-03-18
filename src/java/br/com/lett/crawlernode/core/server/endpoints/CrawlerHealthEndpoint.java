package br.com.lett.crawlernode.core.server.endpoints;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.server.ServerCrawler;
import br.com.lett.crawlernode.util.Logging;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrawlerHealthEndpoint extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerHealthEndpoint.class);


   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
      res.setStatus(HttpServletResponse.SC_OK);

      Logging.printLogDebug(logger, "Received a request on " + ServerCrawler.ENDPOINT_HEALTH_CHECK);
      Logging.printLogDebug(logger, "Health: " + ServerCrawler.MSG_SERVER_HEALTH_OK);
      res.getWriter().println(ServerCrawler.MSG_SERVER_HEALTH_OK);

  }

}
