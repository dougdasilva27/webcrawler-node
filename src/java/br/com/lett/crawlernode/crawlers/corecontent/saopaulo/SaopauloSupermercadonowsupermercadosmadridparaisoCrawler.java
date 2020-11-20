package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosmadridparaisoCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosmadridparaisoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-madrid-paraiso";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Madrid";
   }
}