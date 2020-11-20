package br.com.lett.crawlernode.crawlers.corecontent.guarulhos;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class GuarulhosSupermercadonowsupermercadoslopespontegrandeCrawler extends SupermercadonowCrawler {


   public GuarulhosSupermercadonowsupermercadoslopespontegrandeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-lopes-ponte-grande";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Lopes";
   }
}