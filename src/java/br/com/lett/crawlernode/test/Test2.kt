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
import br.com.lett.crawlernode.test.Test.*

enum class TestType {
   KEYWORDS, INSIGHTS
}

fun main() {

   // if city is empty, fetch by marketId
   // fill city and market or marketId
   val city = ""
   val marketName = ""

   val marketId: Long = 440 //martins

   val urls = listOf(
      ""
   )
   val keywords = listOf(
      ""
   )

   val currentTest = TestType.INSIGHTS

   TestUtils.initialize()

   val parameters = if (currentTest == TestType.KEYWORDS) keywords else urls

   TestUtils.taskProcess(city = city, marketName = marketName, marketId = marketId, parameters = parameters, currentTest = currentTest)
}
