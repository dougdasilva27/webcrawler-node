package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class SaopauloCornershopcarrefourCrawler extends CornershopCrawler {

   public SaopauloCornershopcarrefourCrawler(Session session) {
      super(session);
   }

   public static final String STORE_ID = "6555";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}