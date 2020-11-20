package br.com.lett.crawlernode.crawlers.ranking.keywords.saojosedospinhais;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class SaojosedospinhaisMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public SaojosedospinhaisMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "11";
   }
}
