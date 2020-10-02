package br.com.lett.crawlernode.crawlers.ranking.keywords.brusque;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BistekCrawler;

public class BrusqueBistekCrawler extends BistekCrawler {

   private static final String LOCATION = "07";

   public BrusqueBistekCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLocation() {
      return "loja" + LOCATION;
   }
}
