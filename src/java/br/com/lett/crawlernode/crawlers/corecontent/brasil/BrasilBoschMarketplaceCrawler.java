package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

public class BrasilBoschMarketplaceCrawler extends VTEXNewScraper {
   public BrasilBoschMarketplaceCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList(session.getOptions().optString("seller"));
   }

   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      String description = JSONUtils.getValueRecursive(productJson, "description", ".", String.class, "");
      if (description.equals("")) {
         String complementName = JSONUtils.getValueRecursive(productJson, "items.0.complementName", ".", String.class, "");
         String nameComplete = JSONUtils.getValueRecursive(productJson, "items.0.nameComplete", ".", String.class, "");

         if (!complementName.isEmpty() && !complementName.equals(nameComplete)) {
            description = description.concat(complementName);
         }
      }

      if (productJson.has("Descrição do Produto (A+)")) {
         String descriptionComplete = JSONUtils.getValueRecursive(productJson, "Descrição do Produto (A+).0", ".", String.class, "");
         return description.concat(descriptionComplete);
      }

      return description;
   }

   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = JSONUtils.getValueRecursive(productJson, "items.0.nameComplete", ".", String.class, null);

      if (name == null && jsonSku.has("productName") && jsonSku.opt("productName") != null) {
         name = jsonSku.optString("productName");

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
