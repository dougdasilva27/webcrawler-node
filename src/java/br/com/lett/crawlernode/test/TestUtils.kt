package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.models.Market
import br.com.lett.crawlernode.core.models.Markets
import br.com.lett.crawlernode.core.server.PoolExecutor
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.session.SessionFactory
import br.com.lett.crawlernode.core.task.base.RejectedTaskHandler
import br.com.lett.crawlernode.core.task.base.TaskFactory
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter
import br.com.lett.crawlernode.database.DatabaseDataFetcher
import br.com.lett.crawlernode.database.DatabaseManager
import br.com.lett.crawlernode.main.ExecutionParameters
import br.com.lett.crawlernode.main.GlobalConfigurations
import br.com.lett.crawlernode.processor.ResultManager
import br.com.lett.crawlernode.core.task.base.Task
import br.com.lett.crawlernode.core.task.impl.CrawlerRanking
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class TestUtils {
   companion object {

      fun initialize() {

         GlobalConfigurations.executionParameters = ExecutionParameters()

         GlobalConfigurations.executionParameters.setUpExecutionParameters()

         val dbCredentials = DatabaseCredentialsSetter.setCredentials()

         GlobalConfigurations.dbManager = DatabaseManager(dbCredentials)

         GlobalConfigurations.markets = Markets(GlobalConfigurations.dbManager)

         GlobalConfigurations.processorResultManager = ResultManager(GlobalConfigurations.dbManager)

         GlobalConfigurations.proxies = ProxyCollection(GlobalConfigurations.markets, GlobalConfigurations.dbManager)
      }

      fun taskProcess(city: String = "", marketName: String = "", marketId: Long = 0, parameters: List<String>, currentTest: TestType = TestType.INSIGHTS, productsLimit: Int = 0): List<Task> {

         val market: Market? = fetchMarket(city, marketName, marketId)

         val sessionFunc: (String, Market) -> Session

         if (market != null) {

            if (currentTest == TestType.KEYWORDS) {
               Test.testType = "keywords"
               sessionFunc = SessionFactory::createTestRankingKeywordsSession
            } else {
               Test.testType = "insights"
               sessionFunc = SessionFactory::createTestSession
            }

         } else {
            println("Market não encontrado no banco!")

            return emptyList()
         }

         return taskProcess(market, parameters, sessionFunc, productsLimit)
      }

      fun taskProcess(market: Market, parameters: List<String>, sessionFunc: (String, Market) -> Session, productsLimit: Int = 0): List<Task> {

         val tasks = mutableListOf<Task>()

         for (parameter: String in parameters) {
            val session = sessionFunc(parameter, market)

            val task = TaskFactory.createTask(session)

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
      fun fetchMarket(city: String = "", marketName: String = "", marketId: Long = 0): Market? {
         val fetcherDAO = DatabaseDataFetcher(GlobalConfigurations.dbManager)

         return if (city.isNotEmpty()) {
            fetcherDAO.fetchMarket(city, marketName)
         } else {
            fetcherDAO.fetchMarket(marketId)
         }
      }


      fun poolTaskProcess(city: String = "", marketName: String = "", marketId: Long = 0, parameters: List<String>, currentTest: TestType = TestType.INSIGHTS, productsLimit: Int = 0, corePoolSize: Int = 1) {

         val executor = PoolExecutor(corePoolSize, corePoolSize, 0L, TimeUnit.SECONDS, LinkedBlockingQueue(PoolExecutor.DEFAULT_BLOQUING_QUEUE_MAX_SIZE), RejectedTaskHandler())
         for (param in parameters) {
            val t = TestRunnable(city, marketName, marketId, listOf(param), currentTest, productsLimit)
            executor.execute(t)
         }
         executor.shutdown()
         while (!executor.isTerminated) {
         }
      }
   }
}