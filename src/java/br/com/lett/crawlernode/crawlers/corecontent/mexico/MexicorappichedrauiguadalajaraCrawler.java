package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicorappichedrauiguadalajaraCrawler extends MexicoRappiCrawler {

   public MexicorappichedrauiguadalajaraCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1923199933";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}