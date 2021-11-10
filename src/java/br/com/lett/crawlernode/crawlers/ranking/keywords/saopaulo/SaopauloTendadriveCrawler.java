package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class SaopauloTendadriveCrawler extends CrawlerRankingKeywords {

   private final String storeId = getStoreId();

   public String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   public SaopauloTendadriveCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      String url =
            "https://tenda-api.stoomlab.com.br/api/public/store/search?query="
                  + keywordEncoded
                  + "&page="
                  + currentPage
                  + "&order=relevance&save=true";

      JSONObject search = fetchJSONObject(url, cookies);

      JSONArray products = search.optJSONArray("products");

      if (products != null && products.length() > 0) {
         pageSize = search.optInt("products_per_page");
         if (this.totalProducts == 0) {
            totalProducts = search.optInt("total_products");
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject productJson = products.optJSONObject(i);

            String productId = productJson.optString("sku");
            String productUrl =
                  "https://www.tendaatacado.com.br/produto/" + productJson.optString("token", null);

            String name = productJson.optString("name");
            String imageUrl = productJson.optString("thumbnail");
            int price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getDoubleValueFromJSON(productJson,"price", true),0);
            boolean isAvailable = getProductAvailable(productJson);

            //New way to send products to save data product
            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(productId)
               .setInternalPid(null)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            this.log(
                  "Position: " + this.position + " - InternalId: " + productId + " - Url: " + productUrl);
         }
      }
   }

   private Integer getStockFromStoreSpecific(JSONObject productJson){
      int stock = 0;
      JSONArray inventory = productJson.optJSONArray("inventory");
      for (Object o : inventory){
         if (o instanceof JSONObject){
            JSONObject idsStore = (JSONObject) o;
            String branchId = idsStore.optString("branchId");
            if (branchId.equals(storeId)){
               stock = idsStore.optInt("totalAvailable");
               break;
            }
         }
      }

      return stock;
   }

   private boolean getProductAvailable(JSONObject productJson){
      int stock = getStockFromStoreSpecific(productJson);
      return stock > 1;
   }


}
