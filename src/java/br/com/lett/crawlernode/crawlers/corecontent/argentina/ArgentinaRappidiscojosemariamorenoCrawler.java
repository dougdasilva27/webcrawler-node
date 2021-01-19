package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscojosemariamorenoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Chile 502, C1098 AAL, Buenos Aires, Argentina";
   public static final String STORE_ID = "113910";

   public ArgentinaRappidiscojosemariamorenoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}
