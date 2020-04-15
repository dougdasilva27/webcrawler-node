package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadosipirangaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadosipirangaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-agostino-gomes";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}