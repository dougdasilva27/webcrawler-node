package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVACrawler;

public class SaopauloPontofrioCrawler extends CNOVACrawler {

   private static final String MAIN_SELLER_NAME_LOWER = "pontofrio";
   private static final String MAIN_SELLER_NAME_LOWER_2 = "pontofrio.com";
   private static final String HOST = "www.pontofrio.com.br";

   public SaopauloPontofrioCrawler(Session session) {
      super(session);
      super.mainSellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.mainSellerNameLower2 = MAIN_SELLER_NAME_LOWER_2;
      super.marketHost = HOST;
   }
}
