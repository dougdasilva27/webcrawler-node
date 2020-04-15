package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosyamauchivilabelaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosyamauchivilabelaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-yamauchi-vila-bela";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Yamauchi";
   }
}