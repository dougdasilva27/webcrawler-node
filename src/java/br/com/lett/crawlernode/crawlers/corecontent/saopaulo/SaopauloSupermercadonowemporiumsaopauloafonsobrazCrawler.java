package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowemporiumsaopauloafonsobrazCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowemporiumsaopauloafonsobrazCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "emporium-sao-paulo-vila-nova-afonso-braz";
   }

   @Override
   protected String getSellerFullName() {
      return "Emporium São Paulo";
   }
}