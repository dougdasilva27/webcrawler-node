package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosmadridhigienopolisCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosmadridhigienopolisCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-madrid-higienopolis";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Madrid";
   }
}