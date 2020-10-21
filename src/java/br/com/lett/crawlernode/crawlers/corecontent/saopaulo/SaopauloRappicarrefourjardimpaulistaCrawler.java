package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappicarrefourjardimpaulistaCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "carrefour";
   private static final String LOCATION = "lat=-23.5668679&lng=-46.6567335";

   public SaopauloRappicarrefourjardimpaulistaCrawler(Session session) {
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
