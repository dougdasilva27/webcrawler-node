package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiapalermoCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Buenos   Aires -  Raúl Scalabrini Ortiz 3170 -  C1425 DBZ -  Buenos Aires -  Argentina";
   public static final String STORE_ID = "136211";

   public ArgentinaRappidiapalermoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}