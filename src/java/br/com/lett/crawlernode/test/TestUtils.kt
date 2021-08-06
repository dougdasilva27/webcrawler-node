package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.models.Market
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.session.SessionFactory
import br.com.lett.crawlernode.core.task.base.Task
import br.com.lett.crawlernode.core.task.base.TaskFactory
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking
import br.com.lett.crawlernode.database.*
import br.com.lett.crawlernode.main.ExecutionParameters
import br.com.lett.crawlernode.main.GlobalConfigurations
import br.com.lett.crawlernode.processor.ResultManager
import br.com.lett.crawlernode.util.CommonMethods
import br.com.lett.crawlernode.util.Logging
import br.com.lett.crawlernode.util.ScraperInformation
import java.sql.Connection
import java.sql.Statement
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

enum class TestType {
   KEYWORDS, CORE, DISCOVERY

}


class TestUtils {
   companion object {

      @JvmStatic
      fun initialize() {

         GlobalConfigurations.executionParameters = ExecutionParameters()

         GlobalConfigurations.executionParameters.setUpExecutionParameters()

         val dbCredentials = DatabaseCredentialsSetter.setCredentials()

         GlobalConfigurations.dbManager = DatabaseManager(dbCredentials)

         GlobalConfigurations.processorResultManager = ResultManager(GlobalConfigurations.dbManager)

         GlobalConfigurations.proxies = ProxyCollection(GlobalConfigurations.dbManager)
      }


      @JvmStatic
      fun taskProcess(market: Market, parameters: List<String>, currentTest: TestType = TestType.CORE, productsLimit: Int = 0): List<Task> {

         val scraperInformation = fetchScraperInfoToOneMarket(market.number,currentTest)

         val sessionFunc: (String, Market,ScraperInformation) -> Session

         if (currentTest == TestType.KEYWORDS) {
            Test.testType = "keywords"
            sessionFunc = SessionFactory::createTestRankingKeywordsSession
         } else {
            Test.testType = "insights"
            sessionFunc = SessionFactory::createTestSession
         }



         return taskProcess(market, parameters, sessionFunc, productsLimit, scraperInformation!!)
      }

      fun taskProcess(market: Market, parameters: List<String>, sessionFunc: (String, Market,ScraperInformation) -> Session, productsLimit: Int = 0, scraperInformation: ScraperInformation): List<Task> {

         val tasks = mutableListOf<Task>()

         for (parameter: String in parameters) {
            val session = sessionFunc(parameter, market,scraperInformation)

            val task = TaskFactory.createTask(session, scraperInformation.className)

            if (task != null) {
               if (productsLimit > 0 && sessionFunc == SessionFactory::createTestRankingKeywordsSession) {
                  if (task is CrawlerRanking) {
                     task.setProductsLimit(productsLimit)
                  }
               }
               task.process()
               tasks.add(task)
            } else {
               println("Create Task Error")
            }
         }

         return tasks
      }


      // fetch market
      // if city is empty, fetch by marketId
      @JvmStatic
      fun fetchMarket(city: String = "", marketName: String = "", marketId: Long = 0): Market? {
         val fetcherDAO = DatabaseDataFetcher(GlobalConfigurations.dbManager)

         return if (city.isNotEmpty()) {
            fetcherDAO.fetchMarket(city, marketName)
         } else {
            fetcherDAO.fetchMarket(marketId)
         }
      }


      fun poolTaskProcess(market: Market, parameters: List<String>, currentTest: TestType = TestType.CORE, productsLimit: Int = 0, corePoolSize: Int = 1): List<TestRunnable> {
         val tests: MutableList<TestRunnable> = mutableListOf()

         val executor = Executors.newFixedThreadPool(corePoolSize)

         for (param in parameters) {
            val t = TestRunnable(market, listOf(param), currentTest, productsLimit)
            executor.submit(t)
            tests.add(t)
         }

         executor.shutdown()
         executor.awaitTermination(24L, TimeUnit.HOURS)
         return tests
      }

      fun fetchScraperInfoToOneMarket(marketId: Int,testType: TestType): ScraperInformation? {

         var type : String

         if(testType.equals(TestType.KEYWORDS)){
            type = "RANKING"
         }else{
            type = "CORE"
         }

         var conn: Connection? = null
         var sta: Statement? = null
         var scraperInformation: ScraperInformation? = null
         try {
            val query = "WITH market_informations AS (" +
               "SELECT scraper.market_id, scraper.\"options\" , scraper.active, scraper.scraper_class_id, scraper.\"type\", " +
               "scraper.use_browser, market.fullname, market.first_party_regex, market.code, market.name, market.proxies " +
               "FROM market JOIN scraper ON (market.id = scraper.market_id) " +
               "AND market.id = '" + marketId + "') " +
               "SELECT market_informations.\"options\" as options_scraper, market_informations.active, scraper_class.\"options\", " +
               "public.scraper_class.\"class\", market_informations.market_id, market_informations.use_browser, " +
               "market_informations.first_party_regex, market_informations.code, market_informations.fullname, market_informations.proxies, market_informations.name " +
               "FROM market_informations JOIN scraper_class " +
               "ON (market_informations.scraper_class_id = scraper_class.id) " +
               "WHERE market_informations.active = true and market_informations.\"type\" = '"+ type +"'"
            conn = JdbcConnectionFactory.getInstance().connection
            sta = conn.createStatement()
            val rs = sta.executeQuery(query)
            while (rs.next()) {
               scraperInformation = CommonMethods.getScraperInformation(rs)
            }
         } catch (e: Exception) {
            println("fetch scarper error")
         } finally {
            JdbcConnectionFactory.closeResource(sta)
            JdbcConnectionFactory.closeResource(conn)
         }
         return scraperInformation
      }

   }
}

