package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosyamauchimoocaCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosyamauchimoocaCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-yamauchi-mooca";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Yamauchi";
   }
}