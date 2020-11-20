package br.com.lett.crawlernode.crawlers.ranking.keywords.cascavel;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class CascavelMuffatocomercialCrawler extends SupermuffatoDeliveryCrawler {

   public CascavelMuffatocomercialCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "17";
   }
}
