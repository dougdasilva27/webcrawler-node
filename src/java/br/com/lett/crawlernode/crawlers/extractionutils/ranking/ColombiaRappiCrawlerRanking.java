package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

/**
 * Date: 05/11/20
 *
 * @author Fellype Layunne
 */
public class ColombiaRappiCrawlerRanking extends RappiCrawlerRanking {

   public ColombiaRappiCrawlerRanking(Session session) {
      super(session);
      PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/producto/";

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
      return "https://www.rappi.com.co/tiendas/";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.co/products";
   }
}
