package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappimamboexpressvilaromanaCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "mambo_express";
   private static final String LOCATION = "lat=-23.5376576&lng=-46.6975506";
   private static final String STORE_ID = "900136785";

   public SaopauloRappimamboexpressvilaromanaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String setStoreType() {
      return STORE_TYPE;
   }

   @Override
   protected String setStoreId() {
      return STORE_ID;
   }

   @Override
   protected String setLocationParameters() {
      return LOCATION;
   }
}
