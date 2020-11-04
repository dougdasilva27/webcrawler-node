package br.com.lett.crawlernode.crawlers.corecontent.itaquaquecetuba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class ItaquaquecetubaSupermercadonowveranCrawler extends SupermercadonowCrawler {


   public ItaquaquecetubaSupermercadonowveranCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "veran-supermercados-itaquaquecetuba";
   }

   @Override
   protected String getSellerFullName() {
      return "Veran";
   }
}