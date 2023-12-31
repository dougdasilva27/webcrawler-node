package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.main.EnvironmentVariables;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;

public class Test {

   public static String pathWrite = System.getenv(EnvironmentVariables.HTML_PATH);
   public static String testType = null;

   public static void main(String[] args) throws InterruptedException {
      TestUtils.initialize();

      Dotenv dotenv = Dotenv.configure()
         .directory("src/java/br/com/lett/crawlernode/test")
         .filename("test.env")
         .load();

      String testType = dotenv.get("TEST_TYPE");
      String marketId = dotenv.get("MARKET_ID");
      String marketName = dotenv.get("MARKET_NAME");
      String city = dotenv.get("MARKET_CITY");
      String parameters = dotenv.get("PARAMETERS");
      String maxProducts = dotenv.get("MAX_PRODUCTS");
      String corePollSize = dotenv.get("CORE_POOL_SIZE");
      String fileMiranha = dotenv.get("FILE_MIRANHA");

      Market market = TestUtils.fetchMarket(city != null ? city : "", marketName != null ? marketName : "", Long.parseLong(marketId != null ? marketId : "0"));


      if (market != null) {
         if (testType.equals("DISCOVERY")) {
           new LocalDiscovery().discovery(market, List.of(parameters), Integer.parseInt(maxProducts != null ? maxProducts : "1"),Integer.parseInt(corePollSize != null ? corePollSize : "1"));
         } else {
            TestUtils.taskProcess(market, List.of(parameters), fileMiranha, TestType.valueOf(testType), 0);
         }
      }


   }

}
