package br.com.lett.crawlernode.crawlers.ranking.keywords.londrina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class LondrinaMuffatolondrinaCrawler extends SupermuffatoDeliveryCrawler {

   public LondrinaMuffatolondrinaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "15";
   }
}
