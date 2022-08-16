package br.com.lett.crawlernode.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.processor.ResultManager;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.models.DBCredentials;

public class GlobalConfigurations {

   private static final Logger LOGGER = LoggerFactory.getLogger(GlobalConfigurations.class);

   public static ExecutionParameters executionParameters;
   public static DatabaseManager dbManager;
   public static ProxyCollection proxies;
   public static ResultManager processorResultManager;

   public static DBCredentials dbCredentials;

   public static void setConfigurations() {
      Logging.printLogInfo(LOGGER, "Starting webcrawler-node...");

      // setting execution parameters
      executionParameters = new ExecutionParameters();
      executionParameters.setUpExecutionParameters();

      // setting database credentials
      dbCredentials = new DBCredentials();

      try {
         dbCredentials = DatabaseCredentialsSetter.setCredentials();
      } catch (Exception e) {
         Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
      }

      // creating the database manager
      dbManager = new DatabaseManager(dbCredentials);

      // fetch all markets information from database

      // create result manager for processor stage
      processorResultManager = new ResultManager(dbManager);

      // fetching proxies
      proxies = new ProxyCollection( dbManager);
   }
}
