package br.com.lett.crawlernode.crawlers.ranking.keywords.fozdoiguaçu;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class FozdoiguaçUMuffatojkCrawler extends SupermuffatoDeliveryCrawler {

   public FozdoiguaçUMuffatojkCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "3";
   }
}
