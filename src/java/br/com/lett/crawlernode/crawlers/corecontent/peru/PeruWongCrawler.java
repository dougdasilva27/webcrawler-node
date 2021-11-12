package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public class PeruWongCrawler extends VTEXOldScraper {

   public PeruWongCrawler(Session session) {
      super(session);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String brand = productJson.optString("brand");
      String name = "";

      if (jsonSku.has("nameComplete")) {
         name = jsonSku.get("nameComplete").toString();
      } else if (jsonSku.has("name")) {
         name = jsonSku.get("name").toString();
      } else {
         return null;
      }

      if (brand != null && !brand.equals(".")){
         return name + " - " + brand;
      }
      return name;
   }
   @Override
   protected String getHomePage() {
      return "https://www.wong.pe/";
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList("metro", "wong", "metrofood","wongfood");
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
