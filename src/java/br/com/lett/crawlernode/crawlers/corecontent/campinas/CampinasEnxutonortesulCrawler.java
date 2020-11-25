package br.com.lett.crawlernode.crawlers.corecontent.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.EnxutoSupermercadosCrawler;

public class CampinasEnxutonortesulCrawler extends EnxutoSupermercadosCrawler {
   public static final String STORE_ID = "2401018990188651121:-6738924489776886295";

   public CampinasEnxutonortesulCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
