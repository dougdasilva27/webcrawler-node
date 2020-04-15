package br.com.lett.crawlernode.crawlers.corecontent.sorocaba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SorocabaSupermercadonowsupermercadoslopesmoinhoCrawler extends SupermercadonowCrawler {


   public SorocabaSupermercadonowsupermercadoslopesmoinhoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-sorocaba-moinho";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}