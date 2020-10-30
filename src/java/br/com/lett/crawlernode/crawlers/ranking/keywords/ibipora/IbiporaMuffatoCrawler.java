package br.com.lett.crawlernode.crawlers.ranking.keywords.ibipora;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class IbiporaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public IbiporaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "34";
   }
}
