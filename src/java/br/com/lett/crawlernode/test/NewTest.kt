package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.integration.redis.Redis
import br.com.lett.crawlernode.test.TestUtils.Companion.initialize
import br.com.lett.crawlernode.test.TestUtils.Companion.fetchMarket
import br.com.lett.crawlernode.test.TestUtils.Companion.taskProcess
import kotlin.jvm.JvmStatic
import io.github.cdimascio.dotenv.Dotenv.configure
import io.github.cdimascio.dotenv.dotenv


object NewTest {

    private const val DISCOVERY_TEST = "DISCOVERY"

   @JvmStatic
    fun main(args: Array<String>) {
        initialize()

        val dotenv = dotenv {
           directory = "src/java/br/com/lett/crawlernode/test"
           filename = "test.env"
        }

        val testType = dotenv.get("TEST_TYPE")
        val marketId = dotenv.get("MARKET_ID")
        val marketName = dotenv.get("MARKET_NAME")
        val city = dotenv.get("MARKET_CITY")
        val parameters = dotenv.get("PARAMETERS")
        val maxProducts = dotenv.get("MAX_PRODUCTS")
        val corePollSize = dotenv.get("CORE_POOL_SIZE")

        val market = fetchMarket(city, marketName, marketId?.toLong() ?: 0)


      if (market != null) {
         if (testType == DISCOVERY_TEST) {
            LocalDiscovery().discovery(market, listOf(parameters), maxProducts.toInt(), corePollSize.toInt())
         } else {
            taskProcess(market, listOf(parameters), TestType.valueOf(testType), 0)
         }
      }
      Redis.shutdown()
    }
}
