package br.com.lett.crawlernode.crawlers.ranking.keywords.campinas;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.EnxutoSupermercadosCrawler;

public class CampinasEnxutoprimeacquaCrawler extends EnxutoSupermercadosCrawler {
   public static final String STORE_ID = "8784824704100401917:5454322176772916136";

   public CampinasEnxutoprimeacquaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
