package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchemoemaCrawler extends BrasilRappiCrawler {

   public SaopauloRappistmarchemoemaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return "st_marche";
   }

   @Override
   protected String setLocationParameters() {
      return "lat=-23.5990071&lng=-46.668074";
   }
}
