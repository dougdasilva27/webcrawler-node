package br.com.lett.crawlernode.test;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
import br.com.lett.crawlernode.processor.controller.ResultManager;
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

	public static 	DatabaseManager 	dbManager;
	public static 	ProxyCollection 	proxies;
	public static 	ResultManager 		processorResultManager;
	private static 	Options 			options;
	public static 	Markets				markets;

	private static String market;
	private static String city;
	public static String pathWrite;
	public static String testType;

	public static void main(String args[]) {

		// adding command line options
		options = new Options();
		options.addOption("market", true, "Market name");
		options.addOption("city", true, "City name");
		options.addOption("pathwrite", true, "Path that product html goes");
		options.addOption("testType", true, "Test type [insights, rating, images]");

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
		if (cmd.hasOption("pathwrite")) pathWrite = cmd.getOptionValue("pathwrite"); else { pathWrite = null; }
		if (cmd.hasOption("testType")) testType = cmd.getOptionValue("testType"); else { help(); }

		// setting database credentials
		DBCredentials dbCredentials = DatabaseCredentialsSetter.setCredentials();
		
		// creating the database manager
		dbManager = new DatabaseManager(dbCredentials);

		// create result manager for processor stage
		processorResultManager = new ResultManager(false, dbManager);

		// fetch market information
		Market market = fetchMarket();

		markets = new Markets(dbManager);

		if (market != null) {

			// fetching proxies
			proxies = new ProxyCollection(markets);
			proxies.setCharityProxy();
			proxies.setBonanzaProxies();
			proxies.setBuyProxies();
			proxies.setStormProxies();

			// create a task executor
			// for testing we use 1 thread, there is no need for more
			//taskExecutor = new TaskExecutor(1, 1);

			Session session = SessionFactory.createTestSession("http://www.havan.com.br/banqueta-giratoria-abs-com-regulagem-de-altura-e-encosto-alto-havan/p", market);
			
			Task task = TaskFactory.createTask(session);
			
			task.process();
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
