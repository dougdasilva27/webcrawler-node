package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.main.EnvironmentVariables;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.Logging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

   private static Options options;

   private static String market;
   private static String city;
   public static String pathWrite = System.getenv(EnvironmentVariables.HTML_PATH);
   public static String testType;
   public static String phantomjsPath;

   private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

   public static Map<String, List<Product>> products = new HashMap<>();

   public static void main(String args[]) {
      Logging.printLogInfo(LOGGER, "Starting webcrawler-node...");

      // setting global configuraions
      GlobalConfigurations.setConfigurations();

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


      if (cmd.hasOption("testType"))
         testType = cmd.getOptionValue("testType");
      else {
         help();
      }

      if (cmd.hasOption("phantomjsPath"))
         phantomjsPath = cmd.getOptionValue("phantomjsPath");

      // fetch market information
      Market market = fetchMarket();

      if (market != null) {
         Session session;

         if (testType.equals(KEYWORDS_TEST)) {
            session = SessionFactory.createTestRankingKeywordsSession("comfort ", market);
         } else {
            session = SessionFactory
                  .createTestSession(
                        "https://www.carrefour.com.br/Escova-Dental-Sensodyne-Gentle-Branca-e-Azul/p/9745904",
                        market);
         }

         Task task = TaskFactory.createTask(session);

         task.process();
      } else {
         System.err.println("Market não encontrado no banco!");
      }
   }

   private static Market fetchMarket() {
      DatabaseDataFetcher fetcher = new DatabaseDataFetcher(GlobalConfigurations.dbManager);
      return fetcher.fetchMarket(city, market);
   }

   private static void help() {
      new HelpFormatter().printHelp("Main", options);
      System.exit(0);
   }

}
