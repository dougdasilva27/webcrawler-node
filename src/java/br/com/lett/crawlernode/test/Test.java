package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Samir Leao
 */
public class Test {

	public static final String INSIGHTS_TEST = "insights";
	public static final String RATING_TEST = "rating";
	public static final String IMAGES_TEST = "images";
	public static final String KEYWORDS_TEST = "keywords";
	public static final String CATEGORIES_TEST = "categories";
	private static String market;
	private static String city;
	public static String pathWrite;
	public static String testType;
	public static String phantomjsPath;

	private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);

	public static void main(String[] args) {
		Logging.printLogInfo(LOGGER, "Starting webcrawler-node...");

		GlobalConfigurations.setConfigurations();
		Test.market = "arcomix";
		Test.city = "recife";
		Test.testType = "insights";
		Test.pathWrite = "/home/charl3ff/htmls/";
		Test.phantomjsPath = "/home/charles/workspace/phantomjs-2.1.1/bin/phantomjs";

		Market market = fetchMarket();

		if (market != null) {
			Session session;

			if (testType.equals(KEYWORDS_TEST)) {
				session = SessionFactory.createTestRankingKeywordsSession("leite moça", market);
			} else if (testType.equals(CATEGORIES_TEST)) {
				session = SessionFactory.createTestRankingCategoriesSession(
						"https://www.farmacity.com/panal-huggies-hiperpack-natural-care-ellas/p",
						market, "leite");
			} else {
				session = SessionFactory
						.createTestSession(
								"https://arcomix.com.br/produto/2437/leite-em-po-nestle-ninho-instantaneo-sache-800g",
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
}
