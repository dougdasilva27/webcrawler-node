package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappiatacadaoCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "atacadao";
   private static final String LOCATION = "lng=-46.5836771&lat=-23.5271851";

   public SaopauloRappiatacadaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return STORE_TYPE;
   }

   @Override
   protected String setLocationParameters() {
      return LOCATION;
   }
}
