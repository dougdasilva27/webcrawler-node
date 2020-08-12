
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class BrasilEssencialpravcCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.essencialpravc.com.br/";
   private static final List<String> SELLERS = Arrays.asList("FRN Comunicação Ltda");

   public BrasilEssencialpravcCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected List<String> scrapImages(Document doc, JSONObject skuJson, String internalPid, String internalId) {
      List<String> images = new ArrayList<>();

      for (String key : skuJson.keySet()) {
         if (key.startsWith("images")) {
            JSONArray imagesArray = skuJson.getJSONArray(key);

            for (Object o : imagesArray) {
               JSONObject image = (JSONObject) o;

               String imageLabel = image.optString("imageLabel");

               if (image.has("imageUrl") && !image.isNull("imageUrl") && !"generica".equalsIgnoreCase(imageLabel)) {
                  images.add(CrawlerUtils.completeUrl(image.get("imageUrl").toString(), "https", "jumbo.vteximg.com.br"));
               }
            }

            break;
         }
      }

      return images;
   }


   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      return null;
   }
}
