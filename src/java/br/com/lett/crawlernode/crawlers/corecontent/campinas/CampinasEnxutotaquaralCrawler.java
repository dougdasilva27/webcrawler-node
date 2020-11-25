package br.com.lett.crawlernode.crawlers.corecontent.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.EnxutoSupermercadosCrawler;

public class CampinasEnxutotaquaralCrawler extends EnxutoSupermercadosCrawler {

   public static final String STORE_ID = "8286015187860423704:-5417883741228838516";

   public CampinasEnxutotaquaralCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}

