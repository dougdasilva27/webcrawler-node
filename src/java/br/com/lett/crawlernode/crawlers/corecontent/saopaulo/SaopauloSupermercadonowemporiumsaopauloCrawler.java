package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowemporiumsaopauloCrawler extends SupermercadonowCrawler {
   public SaopauloSupermercadonowemporiumsaopauloCrawler(Session session) {
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
