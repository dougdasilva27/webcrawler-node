package br.com.lett.crawlernode.crawlers.ranking.keywords.presidenteprudente;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class PresidenteprudenteMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public PresidenteprudenteMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "7";
   }
}
