package br.com.lett.crawlernode.crawlers.ranking.keywords.saojosedoriopreto;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SupermuffatoDeliveryCrawler;

public class SaojosedoriopretoSupermuffatodeliveryCrawler extends SupermuffatoDeliveryCrawler {

   public SaojosedoriopretoSupermuffatodeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getCityCode() {
      return "16";
   }

}
