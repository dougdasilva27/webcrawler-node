package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.EnxutoSupermercadosCrawler;

public class CampinasEnxutotorresdobonfimCrawler extends EnxutoSupermercadosCrawler {

   public static final String STORE_ID = "4936341258181468119:-2764428652816758147";

   public CampinasEnxutotorresdobonfimCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
