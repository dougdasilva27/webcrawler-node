package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadoslopesitaqueraCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadoslopesitaqueraCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-itaquera";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}