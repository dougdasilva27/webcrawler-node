package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.EnxutoSupermercadosCrawler;

public class CampinasEnxutojohnboydCrawler extends EnxutoSupermercadosCrawler {
   public static final String STORE_ID = "4929378773374984437:-7046196409751138569";

   public CampinasEnxutojohnboydCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}

