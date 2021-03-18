package br.com.lett.crawlernode.core.server.endpoints;

import br.com.lett.crawlernode.core.server.ServerCrawler;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CrawlerTaskEndpoint {

   protected static final Logger logger = LoggerFactory.getLogger(CrawlerTaskEndpoint.class);

   private CrawlerTaskEndpoint() {
      super();
   }

   public static String perform(HttpServletResponse res, Request request) throws IOException {
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
         res.setStatus(HttpServletResponse.SC_OK);
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
