package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class RiodejaneiroCornershopcarrefourCrawler extends CornershopCrawler {

   public RiodejaneiroCornershopcarrefourCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "7484";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}