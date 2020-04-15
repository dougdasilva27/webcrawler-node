package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.MexicoRappiCrawler;

public class MexicorappihebmonterreyCrawler extends MexicoRappiCrawler {

   public MexicorappihebmonterreyCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1306701619";

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }
}