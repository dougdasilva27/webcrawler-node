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
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.models.DBCredentials;

public class TestImageCrawler {

	public static 	DatabaseManager 	dbManager;
	public static 	ProxyCollection 	proxies;
	private static 	Options 			options;

	private static String marketName;
	private static String cityName;

	private static Logger logger = LoggerFactory.getLogger(TestImageCrawler.class);

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
		if (cmd.hasOption("city")) {
			cityName = cmd.getOptionValue("city"); 
		} else { 
			help(); 
		}

		if (cmd.hasOption("market")) {
			marketName = cmd.getOptionValue("market"); 
		} else { 
			help(); 
		}

		// setting database credentials
		DBCredentials dbCredentials = new DBCredentials();

		try {
			dbCredentials = DatabaseCredentialsSetter.setCredentials();
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTrace(e));
		}

		// creating the database manager
		dbManager = new DatabaseManager(dbCredentials);

		// fetching proxies
		//		proxies = new Proxies();
		//		proxies.setCharityProxy();
		//		proxies.setBonanzaProxies();
		//		proxies.setShaderProxies();
		//		proxies.setBuyProxies();
		//		proxies.setStormProxies();

		// fetch market information
		Market market = fetchMarket();

		//Runnable task = new ImageCrawler(marketName, cityName);
		//taskExecutor.executeTask(task);		
	}

	private static Market fetchMarket() {
		DatabaseDataFetcher fetcher = new DatabaseDataFetcher(dbManager);
		return fetcher.fetchMarket(cityName, marketName);
	}

	private static void help() {
		new HelpFormatter().printHelp("Main", options);
		System.exit(0);
	}

}
