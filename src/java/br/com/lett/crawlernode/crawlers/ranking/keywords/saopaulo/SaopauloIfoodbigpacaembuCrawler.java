package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilIfood;

public class SaopauloIfoodbigpacaembuCrawler extends BrasilIfood {

   public static final String REGION = "sao-paulo-sp";
   public static final String STORE_NAME = "big-pacaembu---express-bom-retiro";
   public static final String STORE_ID = "31dbd467-bb46-4884-8879-e545789acc39";

   public SaopauloIfoodbigpacaembuCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getRegion() {
      return REGION;
   }

   @Override
   protected String getStoreName() {
      return STORE_NAME;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

}
