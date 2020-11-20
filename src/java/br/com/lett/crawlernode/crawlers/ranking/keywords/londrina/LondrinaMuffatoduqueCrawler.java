package br.com.lett.crawlernode.crawlers.ranking.keywords.londrina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class LondrinaMuffatoduqueCrawler extends SupermuffatoDeliveryCrawler {

   public LondrinaMuffatoduqueCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "14";
   }
}
