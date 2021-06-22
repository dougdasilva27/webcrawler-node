package br.com.lett.crawlernode.core.server.endpoints;

import br.com.lett.crawlernode.core.models.RequestConverter;
import br.com.lett.crawlernode.core.server.ServerCrawler;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.server.request.checkers.CrawlerTaskRequestChecker;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.metrics.Exporter;
import br.com.lett.crawlernode.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CrawlerTaskEndpoint extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskEndpoint.class);

   /**
    * Main route
    *
    * @param req http request with data
    * @param res http response
    */
   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse res) {
      String response;
      Request request = RequestConverter.convert(req);

      if (CrawlerTaskRequestChecker.checkRequest(request)) {

         Logging.printLogDebug(logger, request.toString());

         response = Exporter.collectEndpoint(() -> perform(res, request));

      } else {
         response = ServerCrawler.MSG_BAD_REQUEST;
         res.setStatus(ServerCrawler.HTTP_STATUS_CODE_BAD_REQUEST);
      }

      try {
         res.getWriter().println(response);
      } catch (IOException e) {
         logger.error("Could not send response data back");
      }
   }

   public static String perform(HttpServletResponse res, Request request) {
      String response;

      Session session = SessionFactory.createSession(request, request.getMarket());

      Logging.printLogDebug(logger, session, "Creating task for " + session.getOriginalURL());
      Task task = TaskFactory.createTask(session, request.getClassName());

      task.process();

      if (Task.STATUS_COMPLETED.equals(session.getTaskStatus())) {
         response = ServerCrawler.MSG_TASK_COMPLETED;
         res.setStatus(ServerCrawler.HTTP_STATUS_CODE_OK);
      } else {
         response = ServerCrawler.MSG_TASK_FAILED;
         res.setStatus(ServerCrawler.HTTP_STATUS_CODE_SERVER_ERROR);
         Logging.printLogDebug(logger, "TASK RESPONSE STATUS: " + ServerCrawler.HTTP_STATUS_CODE_SERVER_ERROR);
      }

      return response;
   }

}
