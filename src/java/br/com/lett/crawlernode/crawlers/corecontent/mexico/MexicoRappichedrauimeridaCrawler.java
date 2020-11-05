package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappichedrauimeridaCrawler extends MexicoRappiCrawler {

   public MexicoRappichedrauimeridaCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1306705764";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
