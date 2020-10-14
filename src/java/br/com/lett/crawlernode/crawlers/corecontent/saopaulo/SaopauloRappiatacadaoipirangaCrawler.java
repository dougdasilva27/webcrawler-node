package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

public class SaopauloRappiatacadaoipirangaCrawler extends BrasilRappiCrawler {

   private static final String STORE_TYPE = "atacadao";
   private static final String LOCATION = "lng=-23.5778785&lat=-46.6045826";

   public SaopauloRappiatacadaoipirangaCrawler(Session session) {
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

