package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldNewImpl;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class BrasilEpocacosmeticosCrawler extends VTEXOldNewImpl {

   public BrasilEpocacosmeticosCrawler(Session session) {
      super(session);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      String name = null;

      if (productJson.has("productName")) {
         name = productJson.optString("productName");
      }

      if (name == null) {
         if (jsonSku.has("nameComplete") && jsonSku.opt("nameComplete") != null) {
            name = jsonSku.optString("nameComplete");

         } else if (jsonSku.has("name")) {
            name = jsonSku.optString("name");
         }
      }

      if (name != null && !name.isEmpty() && productJson.has("brand")) {
         String brand = productJson.optString("brand");
         if (brand != null && !brand.isEmpty() && !checkIfNameHasBrand(brand, name)) {
            name = name + " " + brand;
         }
      }

      return name;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return new TrustvoxRatingCrawler(session, "393", null).extractRatingAndReviews(internalId, doc, this.dataFetcher);
   }

}
