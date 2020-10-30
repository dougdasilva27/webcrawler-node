package br.com.lett.crawlernode.crawlers.ranking.keywords.pontagrossa;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class PontagrossaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public PontagrossaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "9";
   }
}
