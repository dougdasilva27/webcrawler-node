package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadoscampobeloCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadoscampobeloCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-campo-belo";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}