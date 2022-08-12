package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public class ArgentinaRappiCrawler extends RappiCrawlerRanking {

   public ArgentinaRappiCrawler(Session session) {
      super(session);
      PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/producto/";

   }

   public static final String API_DOMAIN = "rappi.com.ar";
   public static final String PRODUCT_DOMAIN = "rappi.com.ar";

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
      return "https://www.rappi.com.ar/tiendas/";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.ar/products";
   }
}
