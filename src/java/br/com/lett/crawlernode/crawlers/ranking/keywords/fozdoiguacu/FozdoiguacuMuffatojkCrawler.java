package br.com.lett.crawlernode.crawlers.ranking.keywords.fozdoiguacu;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class FozdoiguacuMuffatojkCrawler extends SupermuffatoDeliveryCrawler {

   public FozdoiguacuMuffatojkCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "3";
   }
}
