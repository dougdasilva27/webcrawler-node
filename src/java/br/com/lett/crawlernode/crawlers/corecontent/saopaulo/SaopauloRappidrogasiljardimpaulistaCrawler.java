package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappidrogasiljardimpaulistaCrawler extends BrasilRappiCrawler {

   public SaopauloRappidrogasiljardimpaulistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return "drogasilmarket2";
   }

   @Override
   protected String setLocationParameters() {
      return "lat=-23.584293&lng=-46.674584";
   }
}
