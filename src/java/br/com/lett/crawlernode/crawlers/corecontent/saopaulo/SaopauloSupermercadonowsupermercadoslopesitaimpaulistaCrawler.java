package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadoslopesitaimpaulistaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadoslopesitaimpaulistaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-itaim-paulista";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}