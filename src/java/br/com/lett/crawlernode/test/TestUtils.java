package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.SessionFactory;
import br.com.lett.crawlernode.core.session.ranking.TestRankingKeywordsSession;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking;
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.database.JdbcConnectionFactory;
import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.processor.ResultManager;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.ScraperInformation;
import credentials.models.DBCredentials;
import org.apache.kafka.common.acl.AclOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TestUtils {

   protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);

   public static void initialize() {
      try {

         GlobalConfigurations.executionParameters = new ExecutionParameters();

         GlobalConfigurations.executionParameters.setUpExecutionParameters();

         DBCredentials dbCredentials = DatabaseCredentialsSetter.setCredentials();

         GlobalConfigurations.dbManager = new DatabaseManager(dbCredentials);

         GlobalConfigurations.processorResultManager = new ResultManager(GlobalConfigurations.dbManager);

         GlobalConfigurations.proxies = new ProxyCollection(GlobalConfigurations.dbManager);
      } catch (Exception e) {
         System.out.println("não foi possivel iniciar");
      }
   }

   public static List<Task> taskProcess(Market market, List<String> parameters,  TestType currentTest, Integer productsLimit){
      return taskProcess(market,parameters,null,currentTest,productsLimit);
   }
   public static List<Task> taskProcess(Market market, List<String> parameters, String fileMiranha, TestType currentTest, Integer productsLimit) {

      ScraperInformation scraperInformation = fetchScraperInfoToOneMarket(market.getNumber(), currentTest);


      List<Task> tasks = new ArrayList<>();

      for (String parameter : parameters) {

         Session session;
         if (currentTest == TestType.KEYWORDS) {
            session = SessionFactory.createTestRankingKeywordsSession(parameter, market, scraperInformation);
         } else {
            session = SessionFactory.createTestSession(parameter, market, scraperInformation, fileMiranha);
         }
         String className = scraperInformation.getClassName().split("\\.")[scraperInformation.getClassName().split("\\.").length - 1];
         Logging.printLogDebug(logger, "Crawler running for class: " + ".("+ className +".java:1)");
         Task task = TaskFactory.createTask(session, scraperInformation.getClassName());

         if (task != null) {
            if (productsLimit > 0 && session instanceof TestRankingKeywordsSession) {
               if (task instanceof CrawlerRanking) {
                  ((CrawlerRanking) task).setProductsLimit(productsLimit);
               }
            }
            task.process();
            tasks.add(task);
         } else {
            System.err.println("Create Task Error");
         }
      }

      return tasks;
   }

   public static ScraperInformation fetchScraperInfoToOneMarket(Integer marketId, TestType testType) {

      String type;

      if (testType.equals(TestType.KEYWORDS)) {
         type = "RANKING";
      } else {
         type = "CORE";
      }

      Connection conn = null;
      Statement sta = null;
      ScraperInformation scraperInformation = null;
      try {
         String query = "WITH market_informations AS (" +
            "SELECT scraper.market_id, scraper.\"options\" , scraper.active, scraper.scraper_class_id, scraper.\"type\", " +
            "scraper.use_browser, market.fullname, market.first_party_regex, market.code, market.name, market.proxies " +
            "FROM market JOIN scraper ON (market.id = scraper.market_id) " +
            "AND market.id = '" + marketId + "') " +
            "SELECT market_informations.\"options\" as options_scraper, market_informations.active, scraper_class.\"options\", " +
            "public.scraper_class.\"class\", market_informations.market_id, market_informations.use_browser, " +
            "market_informations.first_party_regex, market_informations.code, market_informations.fullname, market_informations.proxies, market_informations.name " +
            "FROM market_informations JOIN scraper_class " +
            "ON (market_informations.scraper_class_id = scraper_class.id) " +
            "WHERE market_informations.active = true and market_informations.\"type\" = '" + type + "'";
         conn = JdbcConnectionFactory.getInstance().getConnection();
         sta = conn.createStatement();
         ResultSet rs = sta.executeQuery(query);
         while (rs.next()) {
            scraperInformation = CommonMethods.getScraperInformation(rs);
         }
      } catch (Exception e) {
         System.err.println("fetch scarper error");
      } finally {
         JdbcConnectionFactory.closeResource(sta);
         JdbcConnectionFactory.closeResource(conn);
      }
      return scraperInformation;
   }


   // fetch market
   // if city is empty, fetch by marketId
   public static Market fetchMarket(String city, String marketName, Long marketId) {
      DatabaseDataFetcher fetcherDAO = new DatabaseDataFetcher(GlobalConfigurations.dbManager);
      return (city != null && !city.isEmpty()) ? fetcherDAO.fetchMarket(city, marketName) : fetcherDAO.fetchMarket(marketId);

   }


   public static List<TestRunnable> poolTaskProcess(Market market, List<String> parameters, TestType currentTest, Integer productsLimit) throws InterruptedException {
      return poolTaskProcess(market, parameters, currentTest, productsLimit, 1);
   }

   public static List<TestRunnable> poolTaskProcess(Market market, List<String> parameters, TestType currentTest, Integer productsLimit, Integer corePoolSize) throws InterruptedException {
      List<TestRunnable> tests = new ArrayList<>();

      ExecutorService executor = Executors.newFixedThreadPool(corePoolSize);

      for (String param : parameters) {
         TestRunnable t = new TestRunnable(market, Arrays.asList(param), currentTest, productsLimit);
         executor.submit(t);
         tests.add(t);
      }

      executor.shutdown();
      executor.awaitTermination(24L, TimeUnit.HOURS);
      return tests;
   }

   public static String printProduct(Product product) {
      StringBuilder sb = new StringBuilder();

      int secondaryImagesNumber = product.getSecondaryImages() != null ? product.getSecondaryImages().size() : 0;
      int categoriesNumber = (product.getCategory1() != null ? 1 : 0) + (product.getCategory2() != null ? 1 : 0) + (product.getCategory3() != null ? 1 : 0);

      String status = product.getAvailable() ? "available" : (product.getMarketplace() != null && product.getMarketplace().isEmpty()) ? "not available" : "marketplace";

      sb.append("\tinternalId: " + product.getInternalId() + "\n");
      sb.append("\tinternalPid: " + product.getInternalPid() + "\n");
      sb.append("\tname: " + product.getName() + "\n");
      sb.append("\tstatus: " + status + "\n");
      sb.append("\tprice: " + product.getPrice() + "\n");
      sb.append("\tprice bank: " + (product.getPrices() != null ? product.getPrices().getBankTicketPrice() : null) + "\n");
      sb.append("\tprice from: " + (product.getPrices() != null ? product.getPrices().getPriceFrom() : null) + "\n");
      sb.append("\toffers: " + (product.getOffers() == null ? product.getOffers() : product.getOffers().size()) + "\n");
      sb.append("\tprimary image: " + product.getPrimaryImage() + "\n");
      sb.append("\tsecondary images: " + secondaryImagesNumber + "\n");
      sb.append("\tcategories: " + categoriesNumber + "\n");
      sb.append("\tdescription: " + "html code with " + product.getDescription().length() + " characters" + "\n");
      sb.append("\teans: " + (product.getEans() == null ? product.getEans() : product.getEans().toString()) + "\n");



      return sb.toString();
   }
}
