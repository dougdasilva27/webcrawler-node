package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadosvilamadalenaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadosvilamadalenaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-vila-madalena";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}