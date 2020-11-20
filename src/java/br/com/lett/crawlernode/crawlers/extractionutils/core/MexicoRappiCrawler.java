package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public abstract class MexicoRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com.mx/";

   @Override
   protected String getHomeDomain() {
      return "mxgrability.rappi.com";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.mx/products";
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

   @Override
   protected String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description") && json.get("description") instanceof String) {
         String desc = json.getString("description");

         if (desc.replace(" ", "").contains("-PLU")) {
            String descFinal = desc.replace(CommonMethods.getLast(desc.split("-")), "").trim();
            description.append(descFinal.substring(0, descFinal.length() - 2).trim());
         } else {
            description.append(desc);
         }
      }

      return description.toString();
   }
}
