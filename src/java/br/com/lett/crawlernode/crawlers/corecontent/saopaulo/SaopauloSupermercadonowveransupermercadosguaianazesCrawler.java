package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowveransupermercadosguaianazesCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowveransupermercadosguaianazesCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "veran-supermercados-guaianazes";
   }

   @Override
   protected String getSellerFullName() {
      return "Veran Supermercados";
   }
}