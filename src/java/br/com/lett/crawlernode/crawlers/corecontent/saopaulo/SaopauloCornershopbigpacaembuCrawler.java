package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CornershopCrawler;

public class SaopauloCornershopbigpacaembuCrawler extends CornershopCrawler {

   public SaopauloCornershopbigpacaembuCrawler(Session session) {
      super(session);
   }

    public static final String STORE_ID = "5870";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}