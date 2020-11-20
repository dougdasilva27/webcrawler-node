package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappilacomermonterreyCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomermonterreyCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "1307600100";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
