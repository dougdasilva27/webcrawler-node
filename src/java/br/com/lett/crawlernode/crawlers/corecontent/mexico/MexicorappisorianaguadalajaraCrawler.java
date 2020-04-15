package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicorappisorianaguadalajaraCrawler extends MexicoRappiCrawler {

   public MexicorappisorianaguadalajaraCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "160194935";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}