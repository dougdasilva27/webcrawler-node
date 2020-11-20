package br.com.lett.crawlernode.crawlers.ranking.keywords.divinopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.AbcsupermercadosCrawler;

public class DivinopolisAbcsupermercadosCrawler extends AbcsupermercadosCrawler {

   private static final String STORE_ID = "1";

   public DivinopolisAbcsupermercadosCrawler(Session session) {
      super(session);
   }
   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
