package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadosparaisoCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadosparaisoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-paraiso";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}