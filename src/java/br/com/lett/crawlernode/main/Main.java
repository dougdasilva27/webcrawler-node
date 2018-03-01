package br.com.lett.crawlernode.main;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.aws.sqs.QueueHandler;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.server.Server;
import br.com.lett.crawlernode.core.server.ServerExecutorStatusAgent;
import br.com.lett.crawlernode.core.server.ServerExecutorStatusCollector;
import br.com.lett.crawlernode.core.task.Resources;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.processor.ResultManager;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.models.DBCredentials;


/**
 * 
 * Environment variables: DEBUG : to print debug log messages on console [ON/OFF] ENVIRONMENT
 * [development, production]
 * 
 * <p>
 * Environments:
 * </p>
 * <ul>
 * <li>development: in this mode we trigger the program by the Test class and it's main method. This
 * is the fastest and basic mode for testing. It only tests the crawler information extraction
 * logic.</li>
 * <li>production: in this mode we run the Main class and starts the crawler server. It will keep
 * listening on the crawler-task endpoint, under port 5000. To run any task, the user must assemble
 * a POST request (you can use Postman) and send the POST for the server. Running this way, all the
 * process will run (data will be stored in database and all postprocessing will take place after
 * the main information is crawled.)</li>
 * </ul>
 * 
 * @author Samir Leão
 *
 */

public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  public static ExecutionParameters executionParameters;
  public static ProxyCollection proxies;
  public static DBCredentials dbCredentials;
  public static DatabaseManager dbManager;
  public static ResultManager processorResultManager;
  public static QueueHandler queueHandler;
  public static Markets markets;
  public static Resources globalResources;
  public static ServerExecutorStatusAgent serverExecutorStatusAgent;
  public static Server server;

  public static void main(String args[]) {
    Logging.printLogDebug(LOGGER, "Starting webcrawler-node...");

    // setting execution parameters
    executionParameters = new ExecutionParameters();
    executionParameters.setUpExecutionParameters();

    // check resources
    Logging.printLogDebug(LOGGER, "Checking files...");
    checkFiles();

    // setting MDC for logging messages
    Logging.setLogMDC();

    // setting database credentials
    DBCredentials dbCredentials = new DBCredentials();


    try {
      dbCredentials = DatabaseCredentialsSetter.setCredentials();
    } catch (Exception e) {
      Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
    }

    // creating the database manager
    dbManager = new DatabaseManager(dbCredentials);

    // fetch all markets information from database
    markets = new Markets(dbManager);

    // initialize temporary folder for images download
    Persistence.initializeImagesDirectories(markets);

    // create result manager for processor stage
    processorResultManager = new ResultManager(false, dbManager);

    // fetching proxies
    proxies = new ProxyCollection(markets, dbManager);

    // create a queue handler that will contain an Amazon SQS instance
    queueHandler = new QueueHandler();

    // create the server
    server = new Server();

    // create the scheduled task to check the executor status
    serverExecutorStatusAgent = new ServerExecutorStatusAgent();
    serverExecutorStatusAgent.executeScheduled(new ServerExecutorStatusCollector(server), 5);
  }

  private static void checkFiles() {
    File phantom = new File(executionParameters.getPhantomjsPath());
    if (!phantom.exists() && !phantom.isDirectory()) {
      Logging.printLogError(LOGGER, "Phantom webdriver binary not found.");
      System.exit(1);
    }
  }

}
