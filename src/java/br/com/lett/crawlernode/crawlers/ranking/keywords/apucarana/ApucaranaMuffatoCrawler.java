package br.com.lett.crawlernode.crawlers.ranking.keywords.apucarana;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class ApucaranaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public ApucaranaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "5";
   }
}
