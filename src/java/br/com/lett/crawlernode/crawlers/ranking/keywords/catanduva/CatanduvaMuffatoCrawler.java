package br.com.lett.crawlernode.crawlers.ranking.keywords.catanduva;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class CatanduvaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public CatanduvaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "32";
   }
}
