package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappisorianaguadalajaraCrawler extends MexicoRappiCrawler {

   public MexicoRappisorianaguadalajaraCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "160194935";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
