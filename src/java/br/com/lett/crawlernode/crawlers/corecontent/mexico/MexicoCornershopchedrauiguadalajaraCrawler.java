package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CornershopCrawler;

public class MexicoCornershopchedrauiguadalajaraCrawler extends CornershopCrawler {

   public MexicoCornershopchedrauiguadalajaraCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "165";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}