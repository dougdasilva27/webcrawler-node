package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappigigaluzCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "giga";
   private static final String LOCATION = "lat=-23.5196577&lng=-46.6360714";
   public static final String STORE_ID = "900022260";

   public SaopauloRappigigaluzCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
