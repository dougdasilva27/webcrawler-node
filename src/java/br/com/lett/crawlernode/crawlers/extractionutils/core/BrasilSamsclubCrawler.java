package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrasilSamsclubCrawler extends VTEXNewScraper {
   public BrasilSamsclubCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      List<String> sellers = new ArrayList<>();
      JSONArray sellersJSON = session.getOptions().optJSONArray("sellers");

      for (int i = 0; i < sellersJSON.length(); i++) {
         sellers.add(sellersJSON.optString(i));
      }

      return sellers;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      JSONArray descriptionArray = productJson.optJSONArray("Descrição");
      if (descriptionArray != null && !descriptionArray.isEmpty()) {
         for (int i = 0; i < descriptionArray.length(); i++) {
            description.append(descriptionArray.optString(i));
         }
      }

      return description.toString();
   }

}
