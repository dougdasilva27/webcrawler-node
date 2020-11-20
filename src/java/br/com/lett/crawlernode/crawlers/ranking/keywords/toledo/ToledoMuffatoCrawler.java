package br.com.lett.crawlernode.crawlers.ranking.keywords.toledo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class ToledoMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public ToledoMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "10";
   }
}
