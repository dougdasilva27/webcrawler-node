package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class CuritibaMuffatotarumaCrawler extends SupermuffatoDeliveryCrawler {

   public CuritibaMuffatotarumaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "19";
   }
}
