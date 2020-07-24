package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.core.fetcher.ProxyCollection
import br.com.lett.crawlernode.core.models.Market
import br.com.lett.crawlernode.core.models.Markets
import br.com.lett.crawlernode.core.session.SessionFactory
import br.com.lett.crawlernode.core.session.SessionFactory.createTestSession
import br.com.lett.crawlernode.core.task.base.TaskFactory
import br.com.lett.crawlernode.database.DatabaseCredentialsSetter
import br.com.lett.crawlernode.database.DatabaseDataFetcher
import br.com.lett.crawlernode.database.DatabaseManager
import br.com.lett.crawlernode.main.ExecutionParameters
import br.com.lett.crawlernode.main.GlobalConfigurations
import br.com.lett.crawlernode.processor.ResultManager
import br.com.lett.crawlernode.test.Test.*

const val keywordTest = "keyword"

fun main() {
   initialize()
   val city = "colombia"
   val marketName = "exito"
   val url = "https://www.exito.com/maquina-recargableillette-mach3-2-cartuchos-558646/p"
   val keyword = "mascara de cilios"
   testType = "insights"
   // -> "keyword"
   // -> "insights"
   val market: Market? = fetchMarket(city, marketName)

   if (market != null) {
      val session = if (testType == keywordTest) {
         SessionFactory.createTestRankingKeywordsSession(keyword, market)
      } else {
         createTestSession(url, market)
      }
      val task = TaskFactory.createTask(session)
      task ?: println("Not found!")
      task.process()
   } else {
      println("Market não encontrado no banco!")
   }
}

private fun fetchMarket(city: String, market: String): Market? {
   val fetcher = DatabaseDataFetcher(GlobalConfigurations.dbManager)
   return fetcher.fetchMarket(city, market)
}

fun initialize() {
   pathWrite = "/mnt/room/Workspace/work/crawler/htmls/"
   phantomjsPath = "/home/charl3ff/workspace/work/phantomjs/phantomjs"
   GlobalConfigurations.executionParameters = ExecutionParameters()
   GlobalConfigurations.executionParameters.setUpExecutionParameters()

   val dbCredentials = DatabaseCredentialsSetter.setCredentials()

   GlobalConfigurations.dbManager = DatabaseManager(dbCredentials)

   GlobalConfigurations.markets = Markets(GlobalConfigurations.dbManager)

   GlobalConfigurations.processorResultManager = ResultManager(GlobalConfigurations.dbManager)

   // fetching proxies
   GlobalConfigurations.proxies = ProxyCollection(GlobalConfigurations.markets, GlobalConfigurations.dbManager)

//   GlobalConfigurations.proxyService = ProxyService.getInstance()
}
