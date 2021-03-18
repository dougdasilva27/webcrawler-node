package br.com.lett.crawlernode.core.server;

import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

public class ServerCrawler {

   private static final Logger logger = LoggerFactory.getLogger(ServerCrawler.class);

   public static final String MSG_TASK_COMPLETED = "task completed";
   public static final String MSG_TASK_FAILED = "task failed";
   public static final String MSG_METHOD_NOT_ALLOWED = "request method not allowed";
   public static final String MSG_SERVER_HEALTH_OK = "the server is fine";
   public static final String MSG_BAD_REQUEST = "bad request";
   public static final String MSG_TOO_MANY_REQUESTS = "the server is full";
   public static final String MSG_TOO_MANY_REQUESTS_WEBDRIVER = "the server is full for webdriver tasks";

   public static final int HTTP_STATUS_CODE_OK = 200;
   public static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;
   public static final int HTTP_STATUS_CODE_BAD_REQUEST = 400;
   public static final int HTTP_STATUS_CODE_METHOD_NOT_ALLOWED = 402;
   public static final int HTTP_STATUS_CODE_NOT_FOUND = 404;
   public static final int HTTP_STATUS_CODE_TOO_MANY_REQUESTS = 429;

   public static final int DEFAULT_MAX_THREADS = 8;
   public static final int DEFAULT_MIN_THREADS = 5;

   public static final int DEFAULT_IDLE_TIMEOUT = 60000;
   public static final int DEFAULT_BLOCKING_QUEUE_SIZE = 5;

   public static final String ENDPOINT_TASK = "/task";
   public static final String ENDPOINT_TEST = "/test";
   public static final String ENDPOINT_HEALTH_CHECK = "/health-check";

   private static final int SERVER_PORT = 5000;
   private static final String SERVER_HOST = "localhost";

   private Server server;

   public static final int DEFAULT_MAX_WEBDRIVER_INSTANCES = 4;

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

      Logging.printLogDebug(logger, "Creating executor...");
      Logging.printLogDebug(logger, "Done.");

      Logging.printLogDebug(logger, "Creating server [" + SERVER_HOST + "][" + SERVER_PORT + "]...");
      createServer();
      Logging.printLogDebug(logger, "Done.");
   }


   private void createServer() throws Exception {
      try {
         QueuedThreadPool threadPool = new QueuedThreadPool(DEFAULT_MAX_THREADS, DEFAULT_MIN_THREADS, DEFAULT_IDLE_TIMEOUT, new ArrayBlockingQueue<Runnable>(DEFAULT_BLOCKING_QUEUE_SIZE));
         this.server = new Server(threadPool);

         ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
         ServletHandler handler = new ServletHandler();

         connector.setHost(SERVER_HOST);
         connector.setPort(SERVER_PORT);

         server.addConnector(connector);
         server.setHandler(handler);
         handler.addServletWithMapping(ServerHandler.class, ENDPOINT_TASK);
         handler.addServletWithMapping(ServerHandler.class, ENDPOINT_HEALTH_CHECK);
         handler.addServletWithMapping(ServerHandler.class, ENDPOINT_TEST);

         server.start();

         Logging.printLogInfo(logger, "Server started. Listening on port " + SERVER_PORT);

      } catch (
         IOException ex) {
         Logging.printLogError(logger, "error creating server.");
         CommonMethods.getStackTraceString(ex);
      }

   }

   public boolean isAcceptingWebdriverTasks() {
      synchronized (webdriverInstancesCounterLock) {
         return webdriverInstances < DEFAULT_MAX_WEBDRIVER_INSTANCES;
      }
   }

//   public int getActiveTasks() {
//      return executor.getActiveTaskCount();
//   }

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
      synchronized (lock) {
         return succeededTasks;
      }
   }

   public long getFailedTasksCount() {
      synchronized (lock) {
         return failedTasksCount;
      }
   }

//   public int getTaskQueueSize() {
//      return executor.getBloquingQueueSize();
//   }

//   public int getActiveThreads() {
//      return executor.getActiveThreadsCount();
//   }

   public int getWebdriverInstances() {
      synchronized (webdriverInstancesCounterLock) {
         return webdriverInstances;
      }
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

   public void setWebdriverInstances(int webdriverInstances) {
      this.webdriverInstances = webdriverInstances;
   }

}
