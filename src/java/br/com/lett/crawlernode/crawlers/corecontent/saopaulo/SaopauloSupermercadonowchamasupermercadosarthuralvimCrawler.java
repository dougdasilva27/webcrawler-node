package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowchamasupermercadosarthuralvimCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowchamasupermercadosarthuralvimCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-arthur-alvim";
   }

   @Override
   protected String getSellerFullName() {
      return "Chama Supermercados";
   }
}
