package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
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
      this.log("Página " + this.currentPage);

      String url = "https://api.tendaatacado.com.br/api/public/store/search?query=" + this.keywordEncoded + "&page=" + this.currentPage + "&order=relevance&save=true";
      this.log("Link onde são feitos os crawlers: " + url);
      JSONObject search = fetchJSONObject(url);

      JSONArray products = JSONUtils.getJSONArrayValue(search, "products");

      if (search != null) {
         this.pageSize = JSONUtils.getIntegerValueFromJSON(search, "products_per_page", 0);
         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getIntegerValueFromJSON(search, "total_products", 0);
         }
      }

      if (products.length() > 0) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject productJson = products.optJSONObject(i);

            String productId = productJson.optString("sku");
            String productPid = productJson.optString("id");
            String productUrl = scrapUrl(productJson);
            String name = productJson.optString("name");
            String imageUrl = productJson.optString("thumbnail");
            int price = CommonMethods.doublePriceToIntegerPrice(JSONUtils.getDoubleValueFromJSON(productJson,"price", true),0);
            boolean isAvailable = getStockFromStoreSpecific(productJson) > 0;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(productId)
               .setInternalPid(productPid)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .setImageUrl(imageUrl)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String scrapUrl(JSONObject productJson) {
      String urlToken = productJson.optString("token");
      return urlToken != null ? "https://www.tendaatacado.com.br/produto/" + urlToken : null;
   }

   private int getStockFromStoreSpecific(JSONObject productJson){
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
}
