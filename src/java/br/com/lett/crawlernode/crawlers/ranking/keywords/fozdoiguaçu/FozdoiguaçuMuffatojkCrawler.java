package br.com.lett.crawlernode.crawlers.ranking.keywords.fozdoiguaçu;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class FozdoiguaçuMuffatojkCrawler extends SupermuffatoDeliveryCrawler {

   public FozdoiguaçuMuffatojkCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "3";
   }
}
