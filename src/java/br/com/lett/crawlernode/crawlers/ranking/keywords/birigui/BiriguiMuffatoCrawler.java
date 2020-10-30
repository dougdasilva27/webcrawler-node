package br.com.lett.crawlernode.crawlers.ranking.keywords.birigui;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.SupermuffatoDeliveryCrawler;

public class BiriguiMuffatoCrawler extends SupermuffatoDeliveryCrawler {

   public BiriguiMuffatoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "27";
   }
}
