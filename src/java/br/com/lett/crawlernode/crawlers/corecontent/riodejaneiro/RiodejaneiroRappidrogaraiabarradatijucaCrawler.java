package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class RiodejaneiroRappidrogaraiabarradatijucaCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "raia";
   public static final String STORE_ID = "900006787";

   public RiodejaneiroRappidrogaraiabarradatijucaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
