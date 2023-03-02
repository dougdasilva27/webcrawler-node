package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class BrasilLuitexCrawler extends VTEXNewScraper {
   public BrasilLuitexCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      List<String> sellers = new ArrayList<>();
      JSONArray sellersArray = session.getOptions().optJSONArray("sellers");
      for (Object seller : sellersArray) {
         sellers.add(seller.toString());
      }
      return sellers;
   }

   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = null;

      if (jsonSku.has("nameComplete")) {
         name = jsonSku.optString("nameComplete");
      }

      if (name == null && productJson.has("productName") && productJson.opt("productName") != null) {
         name = productJson.optString("productName");

      } else if (name == null && jsonSku.has("name")) {
         name = jsonSku.optString("name");
      }

      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)) {
            name = name + " " + brand;
         }
      }

      return name;
   }
}
