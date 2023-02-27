package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public class ColombiaRappiRestauranteCrawlerRanking extends RappiRestauranteCrawlerRanking {

   public ColombiaRappiRestauranteCrawlerRanking(Session session) {
      super(session);
   }

   public static final String API_DOMAIN = "grability.rappi.com";
   public static final String PRODUCT_DOMAIN = "rappi.com.co";

   @Override
   protected String getApiDomain() {
      return API_DOMAIN;
   }

   @Override
   protected String getProductDomain() {
      return PRODUCT_DOMAIN;
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.co/restaurantes/";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.co/products";
   }
}
