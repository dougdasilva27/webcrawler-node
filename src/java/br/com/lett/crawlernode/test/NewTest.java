package br.com.lett.crawlernode.test;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Collections;

public class NewTest {

   private static final String DISCOVERY_TEST = "DISCOVERY";

   public static void main(String[] args) {

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

      if(testType.equals(DISCOVERY_TEST)){
         new LocalDiscovery().discovery(Long.parseLong(marketId), Collections.singletonList(parameters),Integer.parseInt(maxProducts),Integer.parseInt(corePollSize) );
      }
      else {
         TestUtils.taskProcess(city, marketName,Long.parseLong(marketId),Collections.singletonList(parameters),TestType.valueOf(testType),0 );
      }


   }
}
