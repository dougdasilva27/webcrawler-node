package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class SaopauloRappimamboexpressvilaromanaCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "mambo_express";
   private static final String LOCATION = "lat=-23.5376576&lng=-46.6975506";
   public static final String STORE_ID = "900136785";

   public SaopauloRappimamboexpressvilaromanaCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
