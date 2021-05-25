package br.com.lett.crawlernode.test

import br.com.lett.crawlernode.database.Persistence
import br.com.lett.crawlernode.main.EnvironmentVariables
import io.github.cdimascio.dotenv.dotenv
import org.apache.commons.cli.*
import org.slf4j.LoggerFactory

/**
 *
 * @author Samir Leao
 */
object Test {
   const val INSIGHTS_TEST = "insights"
   const val RATING_TEST = "rating"
   const val IMAGES_TEST = "images"
   const val KEYWORDS_TEST = "keywords"


   @JvmField
   var pathWrite = System.getenv(EnvironmentVariables.HTML_PATH)

   @JvmField
   var testType: String? = null

   @JvmStatic
   fun main(args: Array<String>) {
      TestUtils.initialize()

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



      val market = TestUtils.fetchMarket(city, marketName, marketId?.toLong() ?: 0)


      if (market != null) {
         if (testType.equals("DISCOVERY")) {
            LocalDiscovery().discovery(market, listOf(parameters), maxProducts.toInt(), corePollSize.toInt())
         } else {
            TestUtils.taskProcess(market, listOf(parameters), TestType.valueOf(testType), 0)
         }
      }
   }
}

