package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SupermercadonowCrawler;

public class SaopauloSupermercadonowsupermercadosyamauchitatuapeCrawler extends SupermercadonowCrawler {


   public SaopauloSupermercadonowsupermercadosyamauchitatuapeCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getLoadUrl() {
      return "supermercados-yamauchi-tatuape";
   }

   @Override
   protected String getSellerFullName() {
      return "Supermercados Yamauchi";
   }
}