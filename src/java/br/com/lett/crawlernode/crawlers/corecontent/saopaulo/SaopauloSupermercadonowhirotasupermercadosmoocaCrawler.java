package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowhirotasupermercadosmoocaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowhirotasupermercadosmoocaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercado-hirota-mooca";
   }

   @Override
   protected String getSellerFullName() {
      return "Hirota Supermercados";
   }
}