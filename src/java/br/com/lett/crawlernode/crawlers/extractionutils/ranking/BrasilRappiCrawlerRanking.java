package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

public abstract class  BrasilRappiCrawlerRanking extends RappiCrawlerRanking {

   public BrasilRappiCrawlerRanking(Session session) {
      super(session);
      PRODUCT_BASE_URL = "https://www."+getProductDomain()+"/produto/";
   }

   public static final String API_DOMAIN = "rappi.com.br";
   public static final String PRODUCT_DOMAIN = "rappi.com.br";

   @Override
   protected String getApiDomain() {
      return API_DOMAIN;
   }

   @Override
   protected String getProductDomain() {
      return PRODUCT_DOMAIN;
   }
}
