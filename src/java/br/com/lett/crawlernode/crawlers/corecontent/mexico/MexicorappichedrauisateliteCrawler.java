package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicorappichedrauisateliteCrawler extends MexicoRappiCrawler {

   public MexicorappichedrauisateliteCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "990005018";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}