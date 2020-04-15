package br.com.lett.crawlernode.crawlers.corecontent.saobernardodocampo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaobernardodocampoSupermercadonowhirotasupermercadossaobernardoCrawler extends SupermercadonowCrawler {


   public SaobernardodocampoSupermercadonowhirotasupermercadossaobernardoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-sao-bernardo";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}