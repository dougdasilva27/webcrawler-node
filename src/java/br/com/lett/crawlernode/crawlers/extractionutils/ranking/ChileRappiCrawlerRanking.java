package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public class ChileRappiCrawlerRanking extends RappiCrawlerRanking {

   public ChileRappiCrawlerRanking(Session session) {
      super(session);
      PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/producto/";

   }

   public static final String API_DOMAIN = "rappi.cl";
   public static final String PRODUCT_DOMAIN = "rappi.cl";

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
      return "https://www.rappi.cl/tiendas/";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.cl/products";
   }
}
