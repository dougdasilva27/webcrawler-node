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

   /**
    *
    * @param req http request
    * @param res http response ok message
    * @throws IOException error writing o response
    */
   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
      res.setStatus(HttpServletResponse.SC_OK);

      res.getWriter().println(ServerCrawler.MSG_SERVER_HEALTH_OK);

  }

}
