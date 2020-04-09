package br.com.lett.crawlernode.crawlers.corecontent.barueri;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class BarueriSupermercadonowsupermercadoslopesbarueriCrawler extends SupermercadonowCrawler {


   public BarueriSupermercadonowsupermercadoslopesbarueriCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-barueri";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}