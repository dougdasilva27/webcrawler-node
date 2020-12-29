package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.ArgentinaRappiCrawler;

public class ArgentinaRappicarrefourexpresscordobaCrawler extends ArgentinaRappiCrawler {

   public static final String STORE_ID = "12692";
   public static final String STORE_TYPE = "carrefour_express";

   public ArgentinaRappicarrefourexpresscordobaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getStoreType() {
      return STORE_TYPE;
   }
}