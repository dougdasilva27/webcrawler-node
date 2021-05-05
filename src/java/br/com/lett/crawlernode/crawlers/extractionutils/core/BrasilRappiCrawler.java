package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;

public abstract class BrasilRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com";

   public BrasilRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomeDomain() {
      return "rappi.com.br";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.br/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "produto";
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }
}
