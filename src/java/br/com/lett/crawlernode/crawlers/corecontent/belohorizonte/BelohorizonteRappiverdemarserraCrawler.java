package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

/**
 * Date: 19/10/20
 *
 * @author Fellype Layunne
 */
public class BelohorizonteRappiverdemarserraCrawler extends BrasilRappiCrawler {
   private static final String STORE_TYPE = "verdemar";
   private static final String LOCATION = "lat=-19.9375636&lng=-43.9216193";
   public static final String STORE_ID = "900020342";

   public BelohorizonteRappiverdemarserraCrawler(Session session) {
      super(session);
      newUnification = true;
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}

