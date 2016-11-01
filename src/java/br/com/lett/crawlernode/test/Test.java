package br.com.lett.crawlernode.test;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import br.com.lett.crawlernode.core.fetcher.Proxies;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Markets;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.TaskExecutor;
import br.com.lett.crawlernode.core.task.TaskFactory;
import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.processor.controller.ResultManager;
import br.com.lett.crawlernode.server.QueueHandler;

/**
 * 
 * @author Samir Leao
 *
 */
public class Test {

	public static 	DBCredentials 		dbCredentials;
	public static 	DatabaseManager 	dbManager;
	public static 	Proxies 			proxies;
	public static 	ResultManager 		processorResultManager;
	private static 	TaskExecutor 		taskExecutor;
	private static 	Options 			options;
	public static 	QueueHandler		queueHandler;
	public static 	Markets				markets;

	private static String market;
	private static String city;

	public static void main(String args[]) {

		// adding command line options
		options = new Options();
		options.addOption("market", true, "Market name");
		options.addOption("city", true, "City name");

		// parsing command line options
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// getting command line options
		if (cmd.hasOption("city")) city = cmd.getOptionValue("city"); else { help(); }
		if (cmd.hasOption("market")) market = cmd.getOptionValue("market"); else { help(); }

		// setting database credentials
		DatabaseCredentialsSetter dbCredentialsSetter = new DatabaseCredentialsSetter("crawler");
		dbCredentials = dbCredentialsSetter.setDatabaseCredentials();

		// creating the database manager
		dbManager = new DatabaseManager(dbCredentials);
		dbManager.connect();

		// create result manager for processor stage
		processorResultManager = new ResultManager(false, dbManager.mongoMongoImages, dbManager);

		// create a queue handler that will contain an Amazon SQS instance
		//queueHandler = new QueueHandler();

		// fetch market information
		Market market = fetchMarket();

		markets = new Markets(dbManager);

		if (market != null) {

			// fetching proxies
			proxies = new Proxies(markets);
			proxies.setCharityProxy();
			proxies.setBonanzaProxies();
			proxies.setBuyProxies();
			proxies.setStormProxies();

			// create a task executor
			// for testing we use 1 thread, there is no need for more
			taskExecutor = new TaskExecutor(1, 1);

			CrawlerSession session = SessionFactory.createSession("https://www.abxclimatizacao.com.br/split-hi-wall-libero-artcool-inverter-v-9-000-btus-frio-220v-269.html", market);
			Runnable task = TaskFactory.createTask(session);
			taskExecutor.executeTask(task);

			taskExecutor.shutDown();
		}
		else {
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
