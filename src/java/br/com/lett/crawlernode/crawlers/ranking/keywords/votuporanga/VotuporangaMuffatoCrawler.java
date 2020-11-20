package br.com.lett.crawlernode.crawlers.ranking.keywords.votuporanga;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class VotuporangaMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public VotuporangaMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "20";
   }
}
