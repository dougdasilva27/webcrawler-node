package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class FlorianopolisRappiangelonijardimatlanticoCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "angeloni";
   private static final String LOCATION = "lat=-27.5792902&lng=-48.588944";
   public static final String STORE_ID = "900049319";

   public FlorianopolisRappiangelonijardimatlanticoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
