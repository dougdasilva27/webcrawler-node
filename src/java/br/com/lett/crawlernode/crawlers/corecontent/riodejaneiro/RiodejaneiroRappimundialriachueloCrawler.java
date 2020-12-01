package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class RiodejaneiroRappimundialriachueloCrawler extends BrasilRappiCrawler {

   private static final String CEP = "20230-015";
   public static final String STORE_ID = "900127219";

   public RiodejaneiroRappimundialriachueloCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
