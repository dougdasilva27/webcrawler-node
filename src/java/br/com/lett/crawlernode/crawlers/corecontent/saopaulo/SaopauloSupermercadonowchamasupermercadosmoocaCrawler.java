package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowchamasupermercadosmoocaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowchamasupermercadosmoocaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-mooca";
   }

   @Override
   protected String getSellerFullName() {
      return "Chama Supermercados";
   }
}
