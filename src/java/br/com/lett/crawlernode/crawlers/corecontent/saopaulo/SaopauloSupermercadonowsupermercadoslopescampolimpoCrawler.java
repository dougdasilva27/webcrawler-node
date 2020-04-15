package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadoslopescampolimpoCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadoslopescampolimpoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-campo-limpo";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}