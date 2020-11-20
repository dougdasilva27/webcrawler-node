package br.com.lett.crawlernode.crawlers.ranking.keywords.cambe;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class CambeMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public CambeMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "33";
   }
}
