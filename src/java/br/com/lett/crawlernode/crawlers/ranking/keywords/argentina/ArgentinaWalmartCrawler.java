package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ArgentinaWalmartCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "www.walmart.com.ar";

   public ArgentinaWalmartCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchProducts() {
      String apiProducts = "https://ucustom.walmart.com.ar/docs/search.json?bucket=walmart_search_stage&family=product&view=default&text="
         + this.keywordEncoded
         + "&window=50&sort=$_substance_value&direction=-1&levels=1&attributes[sales_channel][]=15&page="
         + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + apiProducts);

      Request request = Request.RequestBuilder.create().setUrl(apiProducts).build();

      JSONObject response = JSONUtils.stringToJson(dataFetcher.get(session, request).getBody());

      return response.optJSONObject("data");
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.log("Página " + this.currentPage);

      this.pageSize = 48;

      // Take all data from the json API.
      JSONObject data = fetchProducts();
      JSONArray products = data.optJSONArray("views");

      if (products.length() >= 1) {
         if (this.totalProducts == 0) setTotalProducts(data);

         for (Object o : products) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;

               String internalPid = product.optString("product_id");
               String productUrl = CrawlerUtils.completeUrl(product.optString("permalink"), "https", HOME_PAGE);
               String name = product.optString("title");
               String imageUrl = product.optString("image");
               int price = getPrice(product);
               boolean isAvailable = getAvaibility(product);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(null)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);

               if (this.arrayProducts.size() == productsLimit) break;

            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return arrayProducts.size() < this.totalProducts;
   }

   private void setTotalProducts(JSONObject product) {
      this.totalProducts = product.optInt("count");

      this.log("Total da busca: " + this.totalProducts);
   }

   private boolean getAvaibility(JSONObject product) {
      String available = product.optString("available");
      return available != null && available.equals("true");
   }

   private int getPrice(JSONObject product) {
      String priceTxt = JSONUtils.getValueRecursive(product, "sales_channel_data.0.best_price", String.class);
    //The price is already in cents format -> ex: "best_price":"8251"
      return Integer.parseInt(priceTxt);

   }

}
