package br.com.lett.crawlernode.test;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.base.Task;
import br.com.lett.crawlernode.core.task.base.TaskFactory;
import br.com.lett.crawlernode.database.DatabaseDataFetcher;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.util.Collections;

public class NewTest {

   private static final String CORE_TEST = "CORE";
   private static final String KEYWORDS_TEST = "KEYWORDS";
   private static final String DISCOVERY_TEST = "DISCOVERY";


   public static void main(String[] args) {

      TestUtils.initialize();

      Dotenv dotenv = Dotenv.configure()
         .directory("src/java/br/com/lett/crawlernode/test")
         .filename("test.env")
         .load();

      DatabaseDataFetcher fetcher = new DatabaseDataFetcher(GlobalConfigurations.dbManager);

      String testType = dotenv.get("TEST_TYPE");
      String marketId = dotenv.get("MARKET_ID");
      String marketName = dotenv.get("MARKET_NAME");
      String city = dotenv.get("MARKET_CITY");
      String parameters = dotenv.get("PARAMETERS");
      String maxProducts = dotenv.get("MAX_PRODUCTS");
      String corePollSize = dotenv.get("CORE_POOL_SIZE");

      if(testType.equals(DISCOVERY_TEST)){
         new LocalDiscovery().discovery(Long.parseLong(marketId), Collections.singletonList(parameters),Integer.parseInt(maxProducts),Integer.parseInt(corePollSize) );
      }
      else {
         TestUtils.taskProcess(city, marketName,Long.parseLong(marketId),Collections.singletonList(parameters),TestType.valueOf(testType),0 );
      }


   }
}
