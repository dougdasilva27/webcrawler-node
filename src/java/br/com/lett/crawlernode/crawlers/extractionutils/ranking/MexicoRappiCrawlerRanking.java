package br.com.lett.crawlernode.crawlers.extractionutils.ranking;


import br.com.lett.crawlernode.core.session.Session;

public abstract class MexicoRappiCrawlerRanking extends RappiCrawlerRanking {

   private static final String DOMAIN = "rappi.com.mx";

   public MexicoRappiCrawlerRanking(Session session) {
      super(session);
   }

   @Override
   protected String getApiDomain() {
      return DOMAIN;
   }

   @Override
   protected String getProductDomain() {
      return DOMAIN;
   }
}
