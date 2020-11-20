package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class RiodejaneiroRappidrogaraialeblonCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "raia";
   private static final String LOCATION = "lng=-43.2241748&lat=-22.9846135";
   public static final String STORE_ID = "900006782";

   public RiodejaneiroRappidrogaraialeblonCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
