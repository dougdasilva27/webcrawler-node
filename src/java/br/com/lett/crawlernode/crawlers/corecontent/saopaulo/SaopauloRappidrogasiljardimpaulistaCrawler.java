package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilRappiCrawler;

public class SaopauloRappidrogasiljardimpaulistaCrawler extends BrasilRappiCrawler {

   public static final String STORE_ID = "900130114";

   public SaopauloRappidrogasiljardimpaulistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
