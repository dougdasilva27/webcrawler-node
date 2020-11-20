package br.com.lett.crawlernode.crawlers.ranking.keywords.aracatuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class AracatubaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public AracatubaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "6";
   }
}
