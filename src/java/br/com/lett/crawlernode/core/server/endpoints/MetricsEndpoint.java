package br.com.lett.crawlernode.core.server.endpoints;


import br.com.lett.crawlernode.core.server.ServerCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class MetricsEndpoint extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(MetricsEndpoint.class);

   /**
    *
    * @param req http request
    * @param res http response ok message
    * @throws IOException error writing o response
    */
   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

  }

}
