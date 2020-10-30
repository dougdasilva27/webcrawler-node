package br.com.lett.crawlernode.crawlers.ranking.keywords.campomourao;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class CampomouraoMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public CampomouraoMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "1";
   }
}
