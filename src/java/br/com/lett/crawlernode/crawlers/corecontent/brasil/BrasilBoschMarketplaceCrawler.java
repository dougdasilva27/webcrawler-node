package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
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
      String description = JSONUtils.getValueRecursive(productJson, "TEXTO ABAIXO DO COMPLEMENTO DO TÍTULO:.0", ".", String.class, "");

      if (description.equals("")) {
         String smallDescription = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".section.section__text p"));
         String completeDescription = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".vtex-tabs__content div[class*=\"product-description\"] div"));

         if (!smallDescription.isEmpty() && !smallDescription.equals(completeDescription)) {
            description = description.concat(smallDescription);
         }

         if (smallDescription.isEmpty() && completeDescription.isEmpty()) {
            description = JSONUtils.getValueRecursive(productJson, "Descrição do Produto (A+).0", ".", String.class, "");
         }
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
