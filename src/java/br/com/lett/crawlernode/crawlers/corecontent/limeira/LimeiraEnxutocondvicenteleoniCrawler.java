package br.com.lett.crawlernode.crawlers.corecontent.limeira;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.EnxutoSupermercadosCrawler;

public class LimeiraEnxutocondvicenteleoniCrawler extends EnxutoSupermercadosCrawler {
   public static final String STORE_ID = "-1341750125085314894:2715434282212885228";

   public LimeiraEnxutocondvicenteleoniCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}

