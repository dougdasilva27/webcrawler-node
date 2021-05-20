package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.session.Session;

/**
 * Date: 05/11/20
 *
 * @author Fellype Layunne
 *
 */
public abstract class ArgentinaRappiCrawler extends RappiCrawlerRanking {

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
}
