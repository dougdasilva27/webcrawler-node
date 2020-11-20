package br.com.lett.crawlernode.crawlers.corecontent.saocaetanodosul;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaocaetanodosulSupermercadonowhirotasupermercadossaocaetanoCrawler extends SupermercadonowCrawler {


   public SaocaetanodosulSupermercadonowhirotasupermercadossaocaetanoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-sao-caetano";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}