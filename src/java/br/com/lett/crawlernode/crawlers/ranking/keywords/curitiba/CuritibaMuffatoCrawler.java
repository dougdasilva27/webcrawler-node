package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class CuritibaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public CuritibaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "13";
   }

}
