package br.com.lett.crawlernode.core.server;

import br.com.lett.crawlernode.core.server.endpoints.CrawlerHealthEndpoint;
import br.com.lett.crawlernode.core.server.endpoints.CrawlerTaskEndpoint;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ServerCrawler {

   private static final Logger logger = LoggerFactory.getLogger(ServerCrawler.class);

   public static final String MSG_TASK_COMPLETED = "task completed";
   public static final String MSG_TASK_FAILED = "task failed";
   public static final String MSG_SERVER_HEALTH_OK = "the server is fine";
   public static final String MSG_BAD_REQUEST = "bad request";

   public static final int HTTP_STATUS_CODE_OK = 200;
   public static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;
   public static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;

   public static final String ENDPOINT_TASK = "/task";
   public static final String ENDPOINT_HEALTH_CHECK = "/health-check";

   private static final int SERVER_PORT = 5000;
   private static final String SERVER_HOST = "localhost";


   private Server server;

   private final StatisticsHandler statisticsHandler = new StatisticsHandler();

   private final Object lock = new Object();
   private long succeededTasks;
   private long failedTasksCount;

   private final Object webdriverInstancesCounterLock = new Object();
   private int webdriverInstances;

   public ServerCrawler() throws Exception {
      Logging.printLogDebug(logger, "Initializing values...");
      succeededTasks = 0;
      failedTasksCount = 0;
      webdriverInstances = 0;

      Logging.printLogDebug(logger, "Creating server [" + SERVER_HOST + "][" + SERVER_PORT + "]...");
      createServer();
      Logging.printLogDebug(logger, "Done.");
   }

   private void createServer() throws Exception {
      try {
         QueuedThreadPool threadPool = new QueuedThreadPool(100, GlobalConfigurations.executionParameters.getThreads());
         this.server = new Server(threadPool);

         ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
         ServletHandler handler = new ServletHandler();

         connector.setHost(SERVER_HOST);
         connector.setPort(SERVER_PORT);

         server.addConnector(connector);
         server.setHandler(handler);
         handler.addServletWithMapping(CrawlerTaskEndpoint.class, ENDPOINT_TASK);
         handler.addServletWithMapping(CrawlerHealthEndpoint.class, ENDPOINT_HEALTH_CHECK);

         statisticsHandler.setHandler(server.getHandler());
         server.setHandler(statisticsHandler);

         server.start();

         Logging.printLogInfo(logger, "Server started. Listening on port " + SERVER_PORT);

      } catch (
         IOException ex) {
         Logging.printLogError(logger, "error creating server.");
         CommonMethods.getStackTraceString(ex);
      }

   }

   public int getActiveTasks() {
      return statisticsHandler.getRequestsActive();
   }

   public void incrementSucceededTasks() {
      synchronized (lock) {
         succeededTasks++;
      }
   }

   public void incrementFailedTasks() {
      synchronized (lock) {
         failedTasksCount++;
      }
   }

   public long getSucceededTasks() {
      return succeededTasks;
   }

   public long getFailedTasksCount() {
      return failedTasksCount;
   }

   public int getTaskQueueSize() {
      return statisticsHandler.getAsyncRequestsWaiting();
   }

   public int getActiveThreads() {
      return server.getThreadPool().getThreads();
   }

   public void incrementWebdriverInstances() {
      synchronized (webdriverInstancesCounterLock) {
         Logging.printLogDebug(logger, "Incrementing webdriver instances.");
         webdriverInstances++;
      }
   }

   public void decrementWebdriverInstances() {
      synchronized (webdriverInstancesCounterLock) {
         Logging.printLogDebug(logger, "Decrementing webdriver instances.");
         webdriverInstances--;
      }
   }

}
