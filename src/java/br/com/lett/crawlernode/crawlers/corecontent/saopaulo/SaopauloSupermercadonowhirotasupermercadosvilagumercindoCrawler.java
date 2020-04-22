package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadosvilagumercindoCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadosvilagumercindoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-vila-gumercindo";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}