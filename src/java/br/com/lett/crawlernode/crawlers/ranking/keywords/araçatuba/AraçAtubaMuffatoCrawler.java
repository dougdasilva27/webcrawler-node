package br.com.lett.crawlernode.crawlers.ranking.keywords.araçatuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class AraçAtubaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public AraçAtubaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "6";
   }
}
