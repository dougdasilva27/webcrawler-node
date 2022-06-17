package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

public class MexicoRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com.mx/";

   @Override
   protected String getHomeDomain() {
      return "rappi.com.mx";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.mx/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "producto/";
   }

   @Override
   protected String getHomeCountry() {
      return HOME_PAGE;
   }

   public MexicoRappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }
}
