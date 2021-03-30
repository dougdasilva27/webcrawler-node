package br.com.lett.crawlernode.crawlers.ranking.keywords.itapevi;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.AmericanasmaisCrawler;

public class ItapeviAmericanasmaisitapeviCrawler extends AmericanasmaisCrawler {

   public ItapeviAmericanasmaisitapeviCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "5328";

   @Override
   public String getStoreId() {
      return STORE_ID;
   }

}
