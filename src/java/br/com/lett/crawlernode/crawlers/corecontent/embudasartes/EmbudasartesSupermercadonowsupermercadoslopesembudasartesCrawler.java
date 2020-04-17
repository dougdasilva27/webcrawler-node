package br.com.lett.crawlernode.crawlers.corecontent.embudasartes;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class EmbudasartesSupermercadonowsupermercadoslopesembudasartesCrawler extends SupermercadonowCrawler {


   public EmbudasartesSupermercadonowsupermercadoslopesembudasartesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-embu";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}