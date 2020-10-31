package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappimambopinheirosCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "mambo";
   private static final String LOCATION = "lat=-23.560817&lng=-46.6921159";
   private static final String STORE_ID = "900020814";

   public SaopauloRappimambopinheirosCrawler(Session session) {
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
