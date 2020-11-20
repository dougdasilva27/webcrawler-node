package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadostatuapeCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadostatuapeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-azevedo-soares";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}