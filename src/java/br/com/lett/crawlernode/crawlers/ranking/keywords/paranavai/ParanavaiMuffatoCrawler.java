package br.com.lett.crawlernode.crawlers.ranking.keywords.paranavai;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class ParanavaiMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public ParanavaiMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "12";
   }
}
