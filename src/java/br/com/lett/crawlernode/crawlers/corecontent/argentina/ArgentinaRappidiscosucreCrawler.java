package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.ArgentinaRappiCrawler;

public class ArgentinaRappidiscosucreCrawler extends ArgentinaRappiCrawler {

   public static final String CEP = "Mariscal ANTONIO JOSE DE Sucre 1836 Buenos Aires -  Argentina";
   public static final String STORE_ID = "116063";

   public ArgentinaRappidiscosucreCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }
}