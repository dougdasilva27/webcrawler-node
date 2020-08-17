package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappistmarchemoemaiiCrawler extends BrasilRappiCrawler {

   public SaopauloRappistmarchemoemaiiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return "st_marche";
   }

   @Override
   protected String setLocationParameters() {
      return "lat=-23.6022758&lng=-46.6646497";
   }
}
