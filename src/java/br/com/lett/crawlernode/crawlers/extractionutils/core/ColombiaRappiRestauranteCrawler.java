package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;

public class ColombiaRappiRestauranteCrawler extends RappiRestauranteCrawler {

   public ColombiaRappiRestauranteCrawler(Session session) {
      super(session);
   }


   protected String getHomeDomain() {
      return "grability.rappi.com";
   }

   protected String getImagePrefix() {
      return "images.rappi.com/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.com.co/";
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.co/tiendas/";
   }
}
