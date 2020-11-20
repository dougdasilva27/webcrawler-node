package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowchamasupermercadosjardimdanferCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowchamasupermercadosjardimdanferCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-jardim-danfer";
   }

   @Override
   protected String getSellerFullName() {
      return "Chama Supermercados";
   }
}
