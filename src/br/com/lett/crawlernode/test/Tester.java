package br.com.lett.crawlernode.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;


import br.com.lett.crawlernode.kernel.models.Market;
import br.com.lett.crawlernode.test.db.DatabaseDataFetcher;
import br.com.lett.crawlernode.test.db.DatabaseManager;
import br.com.lett.crawlernode.test.kernel.fetcher.Proxies;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.test.kernel.task.TaskExecutor;
import br.com.lett.crawlernode.test.kernel.task.TaskFactory;
import br.com.lett.crawlernode.test.processor.controller.ResultManager;



/**
 * Class used to test crawlers logic with any desired number of
 * URLs of any desired market.
 * @author Samir Leao
 *
 */
public class Tester {

	private static final Logger logger = LoggerFactory.getLogger(Tester.class);

	public static TestExecutionParameters 	testExecutionParameters;
	public static Proxies 					proxies;
	public static DBCredentials 			dbCredentials;
	public static DatabaseManager 			dbManager;
	public static ResultManager 			processorResultManager;

	private static TaskExecutor 			taskExecutor;
	private static DatabaseDataFetcher		databaseDataFetcher;

	public static void main(String args[]) {

		init(args);
		test("http://www.adias.com.br/produto/ar-condicionado-split-inverter-digital-9000-btus-samsung-220v-frio-ar09hvspbsnn-az-68294", "brasil", "adias");

	}

	private static void test(String url, String marketCity, String marketName) {

		// fetch the market
		Market market = databaseDataFetcher.fetchMarket(marketCity, marketName);

		if (market != null) {

			// create a crawler session
			CrawlerSession session = new CrawlerSession(url, market);

			// create the task
			Runnable task = TaskFactory.createTask(session);

			// submit the task to the executor
			if (task != null) {
				taskExecutor.executeTask(task);
			} else {
				Logging.printLogError(logger, session, "Error: task could not be created.");					
			}
			
			// shutdown the task executor
			taskExecutor.shutDown();

		} else {
			Logging.printLogError(logger, "Market not found.");
		}

	}

	private static void init(String args[]) {
		
		// set up execution parameters
		testExecutionParameters = new TestExecutionParameters(args);
		testExecutionParameters.setUpExecutionParameters();
		
		// set up MDC variables for the logger
		Logging.setLogMDCTest(testExecutionParameters);

		// setting database credentials
		DatabaseCredentialsSetter dbCredentialsSetter = new DatabaseCredentialsSetter("crawler");
		dbCredentials = dbCredentialsSetter.setDatabaseCredentialsTest();

		// creating the database manager
		dbManager = new DatabaseManager(dbCredentials);
		dbManager.connectTest();

		// create result manager for processor stage
		processorResultManager = new ResultManager(false, dbManager.mongoMongoImages, dbManager);

		// fetching proxies
		proxies = new Proxies();
		proxies.fetchLuminatiProxies();
		proxies.fetchBonanzaProxies();
		proxies.fetchShaderProxies();
		proxies.fetchBuyProxies();
		proxies.fetchStormProxies();

		// create a database data fetcher to retrieve data from postgres
		databaseDataFetcher = new DatabaseDataFetcher(dbManager);

		// create a task executor
		Logging.printLogDebug(logger, "Creating task executor with a maximum of " + testExecutionParameters.getNthreads() + " threads...");
		taskExecutor = new TaskExecutor(testExecutionParameters.getNthreads());
	}

}
