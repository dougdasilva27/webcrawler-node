package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosmadridvilabuarqueCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosmadridvilabuarqueCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-madrid-vila-buarque";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Madrid";
   }
}