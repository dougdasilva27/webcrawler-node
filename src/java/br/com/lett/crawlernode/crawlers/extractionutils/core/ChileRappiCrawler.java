package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;

public class ChileRappiCrawler extends RappiCrawler {

   public ChileRappiCrawler(Session session) {
      super(session);
   }


   protected String getHomeDomain() {
      return "rappi.cl";
   }

   protected String getImagePrefix() {
      return "images.rappi.cl/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.cl/";
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.cl/tiendas/";
   }

}
