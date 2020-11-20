package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowemporiumsaopaulojuremaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowemporiumsaopaulojuremaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "emporium-sao-paulo-moema-jurema";
   }

   @Override
   protected String getSellerFullName() {
      return "Emporium São Paulo";
   }
}