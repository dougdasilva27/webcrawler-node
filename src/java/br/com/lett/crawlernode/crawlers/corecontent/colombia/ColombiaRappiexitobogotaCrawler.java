package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ColombiaRappiCrawler;

public class ColombiaRappiexitobogotaCrawler extends ColombiaRappiCrawler {

   public static final String STORE_ID = "6660081";

   public ColombiaRappiexitobogotaCrawler(Session session) {
      super(session);
      newUnification =true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
