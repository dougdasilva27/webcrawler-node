package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.session.Session;

/**
 * Date: 05/11/20
 *
 * @author Fellype Layunne
 *
 */
public abstract class ColombiaRappiCrawlerRanking extends RappiCrawlerRanking {

  public ColombiaRappiCrawlerRanking(Session session) {
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
}
