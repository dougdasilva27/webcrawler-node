package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.models.Market
import br.com.lett.crawlernode.core.models.Markets
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.session.SessionFactory
import br.com.lett.crawlernode.core.session.SessionFactory.createTestSession
import br.com.lett.crawlernode.core.task.base.TaskFactory
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter
import br.com.lett.crawlernode.database.DatabaseDataFetcher
import br.com.lett.crawlernode.database.DatabaseManager
import br.com.lett.crawlernode.main.ExecutionParameters
import br.com.lett.crawlernode.main.GlobalConfigurations
import br.com.lett.crawlernode.processor.ResultManager
import br.com.lett.crawlernode.test.Test.pathWrite
import br.com.lett.crawlernode.test.Test.testType

enum class TestType {
   KEYWORDS, INSIGHTS
}

fun main() {

   // if city is empty, fetch by marketId
   // fill city and market or marketId
   val city = ""
   val marketName = ""

   val marketId: Long = 966 //renner


   val urls = listOf(
      ""
   )
   val keywords = listOf(
      ""
   )

   val currentTest = TestType.INSIGHTS

   // path to html output
   pathWrite = ""


   initialize2()
   taskProcess(city, marketName, marketId, urls, keywords, currentTest)

}

internal fun taskProcess(city: String, marketName: String, marketId: Long, urls: List<String>, keywords: List<String>, currentTest: TestType) {

   val market: Market? = fetchMarket(city, marketName, marketId)

   if (market != null) {

      if (currentTest == TestType.KEYWORDS) {
         testType = "keywords"
         taskProcess(market, keywords, SessionFactory::createTestRankingKeywordsSession)
      } else {
         taskProcess(market, urls, ::createTestSession)
      }

   } else {
      println("Market não encontrado no banco!")
   }
}

internal fun taskProcess(market: Market, parameters: List<String>, sessionFunc: (String, Market) -> Session) {

   for (parameter: String in parameters) {
      val session = sessionFunc(parameter, market)

      val task = TaskFactory.createTask(session)

      task?.process() ?: println("Create Task Error")

   }
}

// fetch market
// if city is empty, fetch by marketId
private fun fetchMarket(city: String, marketName: String, marketId: Long): Market? {
   val fetcherDAO = DatabaseDataFetcher(GlobalConfigurations.dbManager)

   return if (city.isNotEmpty()) {
      fetcherDAO.fetchMarket(city, marketName)
   } else {
      fetcherDAO.fetchMarket(marketId)
   }

}

private fun initialize2() {

   GlobalConfigurations.executionParameters = ExecutionParameters()
   GlobalConfigurations.executionParameters.setUpExecutionParameters()

   val dbCredentials = DatabaseCredentialsSetter.setCredentials()

   GlobalConfigurations.dbManager = DatabaseManager(dbCredentials)

   GlobalConfigurations.markets = Markets(GlobalConfigurations.dbManager)

   GlobalConfigurations.processorResultManager = ResultManager(GlobalConfigurations.dbManager)

   GlobalConfigurations.proxies = ProxyCollection(GlobalConfigurations.markets, GlobalConfigurations.dbManager)
}
