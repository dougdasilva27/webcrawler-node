package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.MexicoRappiCrawler;

public class MexicoRappilacomerpueblaCrawler extends MexicoRappiCrawler {

   public MexicoRappilacomerpueblaCrawler(Session session) {
      super(session);
      newUnification = true;
   }

    public static final String STORE_ID = "990006029";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
