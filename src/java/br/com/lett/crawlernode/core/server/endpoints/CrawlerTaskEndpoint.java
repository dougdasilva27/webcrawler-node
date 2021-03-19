package br.com.lett.crawlernode.core.server.endpoints;

import br.com.lett.crawlernode.core.models.RequestConverter;
import br.com.lett.crawlernode.core.server.ServerCrawler;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.server.request.checkers.CrawlerTaskRequestChecker;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrawlerTaskEndpoint extends HttpServlet {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskEndpoint.class);

   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse res) {
      String response;
      Logging.printLogInfo(logger, "Received a request on " + ServerCrawler.ENDPOINT_TASK);

      Request request = RequestConverter.convert(req);

      if (CrawlerTaskRequestChecker.checkRequest(request)) {

         Logging.printLogDebug(logger, request.toString());

         response = perform(res, request);

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
