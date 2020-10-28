package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class BelohorizonteRappiverdemarserraCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "verdemar";
   private static final String LOCATION = "lat=-19.9375636&lng=-43.9216193";

   public BelohorizonteRappiverdemarserraCrawler(Session session) {
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
