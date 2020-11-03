package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.RappiCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import org.json.JSONObject;

public class ChileRappijumboCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.cl/";
   public static final String STORE_ID = "76";

   @Override
   protected String getStoreId() {
      return STORE_ID;
   }

   @Override
   protected String getHomeDomain() {
      return "rappi.cl";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.cl/products";
   }

   public ChileRappijumboCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
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

         // Because of this link: https://www.rappi.com.co/search?store_type=hiper&query=900187
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
