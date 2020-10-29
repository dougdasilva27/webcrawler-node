package br.com.lett.crawlernode.crawlers.ranking.keywords.maringa;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class MaringaMuffatojoaopaulinoCrawler extends SupermuffatoDeliveryCrawler {

   public MaringaMuffatojoaopaulinoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "2";
   }
}
