package br.com.lett.crawlernode.test;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.processor.ResultManager;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.models.DBCredentials;

/**
 * 
 * @author Samir Leao
 *
 */
public class Test {

  public static final String INSIGHTS_TEST = "insights";
  public static final String RATING_TEST = "rating";
  public static final String IMAGES_TEST = "images";
  public static final String KEYWORDS_TEST = "keywords";
  public static final String CATEGORIES_TEST = "categories";

  public static ExecutionParameters executionParameters;
  public static DatabaseManager dbManager;
  public static ProxyCollection proxies;
  public static ResultManager processorResultManager;
  private static Options options;
  public static Markets markets;

  private static String market;
  private static String city;
  public static String pathWrite;
  public static String testType;
  public static String phantomjsPath;

  private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

  public static void main(String args[]) {
    Logging.printLogInfo(LOGGER, "Starting webcrawler-node...");
    // setting execution parameters
    executionParameters = new ExecutionParameters();
    executionParameters.setUpExecutionParameters();

    // adding command line options
    options = new Options();
    options.addOption("market", true, "Market name");
    options.addOption("city", true, "City name");
    options.addOption("pathwrite", true, "Path that product html goes");
    options.addOption("testType", true, "Test type [insights, rating, images]");
    options.addOption("phantomjsPath", true, "phantonjs");

    // parsing command line options
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      e.printStackTrace();
    }

    // getting command line options
    if (cmd.hasOption("city"))
      city = cmd.getOptionValue("city");
    else {
      help();
    }

    if (cmd.hasOption("market"))
      market = cmd.getOptionValue("market");
    else {
      help();
    }

    if (cmd.hasOption("pathwrite"))
      pathWrite = cmd.getOptionValue("pathwrite");
    else {
      pathWrite = null;
    }

    if (cmd.hasOption("testType"))
      testType = cmd.getOptionValue("testType");
    else {
      help();
    }

    if (cmd.hasOption("phantomjsPath"))
      phantomjsPath = cmd.getOptionValue("phantomjsPath");

    // setting database credentials
    DBCredentials dbCredentials = new DBCredentials();

    try {
      dbCredentials = DatabaseCredentialsSetter.setCredentials();
    } catch (Exception e) {
      Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
    }

    // creating the database manager
    dbManager = new DatabaseManager(dbCredentials);

    // create result manager for processor stage
    processorResultManager = new ResultManager(dbManager);

    // fetch market information
    Market market = fetchMarket();

    markets = new Markets(dbManager);

    if (market != null) {

      // fetching proxies
      proxies = new ProxyCollection(markets, dbManager);

      // create a task executor
      // for testing we use 1 thread, there is no need for more
      // taskExecutor = new TaskExecutor(1, 1);

      Session session;

      if (testType.equals(KEYWORDS_TEST)) {
        session = SessionFactory.createTestRankingKeywordsSession("futebol", market);
      } else if (testType.equals(CATEGORIES_TEST)) {
        session =
            SessionFactory.createTestRankingCategoriesSession("https://www.nutrii.com.br/nutren-senior-mix-de-frutas-200ml", market, "Aparelhos");
      } else {
        session = SessionFactory.createTestSession(
            "https://www.petz.com.br/produto/racao-golden-formula-senior-para-caes-adultos-sabor-frango-e-arroz-15kg-71353", market);
      }


      Task task = TaskFactory.createTask(session);

      task.process();
      try {
        dbManager.connectionPostgreSQL.closeConnection();
      } catch (Exception e) {
        Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
      }
    } else {
      System.err.println("Market não encontrado no banco!");
    }
  }

  private static Market fetchMarket() {
    DatabaseDataFetcher fetcher = new DatabaseDataFetcher(dbManager);
    return fetcher.fetchMarket(city, market);
  }

  private static void help() {
    new HelpFormatter().printHelp("Main", options);
    System.exit(0);
  }

}
