package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class BrasilMuffatoNorteShoppingCrawler extends SupermuffatoDeliveryCrawler {
   public BrasilMuffatoNorteShoppingCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "21";
   }
}
