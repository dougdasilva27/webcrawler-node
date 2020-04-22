package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadoslopesvilapiauiCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadoslopesvilapiauiCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-vila-piaui";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}