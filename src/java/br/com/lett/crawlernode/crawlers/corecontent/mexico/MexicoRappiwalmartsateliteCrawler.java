package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappiwalmartsateliteCrawler extends MexicoRappiCrawler {

   public MexicoRappiwalmartsateliteCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "990006044";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
