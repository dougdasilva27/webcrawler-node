package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappilacomerguadalajaraCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomerguadalajaraCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "990006029";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
