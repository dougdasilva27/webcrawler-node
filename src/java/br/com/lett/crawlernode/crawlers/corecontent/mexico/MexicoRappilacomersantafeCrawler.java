package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicoRappilacomersantafeCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomersantafeCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "990005891";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
