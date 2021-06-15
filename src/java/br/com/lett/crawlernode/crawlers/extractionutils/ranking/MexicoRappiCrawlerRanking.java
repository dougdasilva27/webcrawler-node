package br.com.lett.crawlernode.crawlers.extractionutils.ranking;


import br.com.lett.crawlernode.core.session.Session;

public class MexicoRappiCrawlerRanking extends RappiCrawlerRanking {

   private static final String DOMAIN = "rappi.com.mx";
   private static final String API_DOMAIN = "mxgrability.rappi.com";

   public MexicoRappiCrawlerRanking(Session session) {
      super(session);
      PRODUCT_BASE_URL = "https://www." + getProductDomain() + "/producto/";
   }

   @Override
   protected String getApiDomain() {
      return API_DOMAIN;
   }

   @Override
   protected String getProductDomain() {
      return DOMAIN;
   }
}
