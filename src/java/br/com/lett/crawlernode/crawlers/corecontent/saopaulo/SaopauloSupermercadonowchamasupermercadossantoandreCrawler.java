package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowchamasupermercadossantoandreCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowchamasupermercadossantoandreCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "chama-supermercados-santo-andre";
   }

   @Override
   protected String getSellerFullName() {
      return "Chama Supermercados";
   }
}
