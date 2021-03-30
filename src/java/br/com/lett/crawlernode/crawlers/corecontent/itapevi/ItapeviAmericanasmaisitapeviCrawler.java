package br.com.lett.crawlernode.crawlers.corecontent.itapevi;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmericanasmaisCrawler;

public class ItapeviAmericanasmaisitapeviCrawler extends AmericanasmaisCrawler {

   public ItapeviAmericanasmaisitapeviCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_NAME = "Americanas - Aldeia da Serra Express";
   public static final String STORE_ID = "5328";


   @Override
   public String getSellerName() {
      return SELLER_NAME;
   }

   @Override
   public String getStoreId() {
      return STORE_ID;
   }

}
