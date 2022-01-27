package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.*;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class BrasilFastshopCrawler extends CrawlerRankingKeywords {

   public BrasilFastshopCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      String apiUrl = "https://fastshop-v6.neemu.com/searchapi/v3/search?apiKey=fastshop-v6&secretKey=7V0dpc8ZFxwCRyCROLZ8xA%253D%253D&terms="
         + this.keywordWithoutAccents.replace(" ", "%20") + "&resultsPerPage=9&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + apiUrl);
      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.fastshop.com.br");

      String json = fetchGetFetcher(apiUrl, null, headers, null);

      JSONObject api = new JSONObject(json);
      extractProductFromJSON(api);
   }

   private void extractProductFromJSON(JSONObject api) throws MalformedProductException {
      if (api.has("products")) {
         if (this.totalProducts == 0) {
            setTotalProductsFromJSON(api);
         }

         JSONArray products = api.getJSONArray("products");
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalPid = crawlinternalPid(product);
            String productUrl = crawlProductUrlFromJson(product, internalPid);
            JSONArray internalIds = crawlInternalId(product);
            String name = product.optString("name");
            String imgUrl = crawlImgUrl(product);
            Double price = product.optDouble("price");
            Integer priceInCents = price.intValue() * 100;
            Boolean isAvailable = product.optString("status").equals("AVAILABLE");

            this.position++;

            for (int j = 0; j < internalIds.length(); j++) {
               String internalId = internalIds.getString(j);

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(priceInCents)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);
            }

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }
      }
   }

   @Override
   protected boolean hasNextPage() {
      return this.arrayProducts.size() < this.totalProducts;
   }

   private void setTotalProductsFromJSON(JSONObject api) {
      if (api.has("size") && api.get("size") instanceof Integer) {
         this.totalProducts = api.getInt("size");
         this.log("Total de produtos: " + this.totalProducts);
      }
   }

   private JSONArray crawlInternalId(JSONObject product) {
      JSONArray internalIds = new JSONArray();

      if (product.has("details")) {
         JSONObject details = product.getJSONObject("details");

         if (details.has("catalogEntryId")) {
            internalIds = details.getJSONArray("catalogEntryId");
         }
      }

      return internalIds;
   }

   private String crawlinternalPid(JSONObject product) {
      String pid = null;

      if (product.has("id")) {
         pid = product.get("id").toString();
      }

      return pid;
   }

   private String crawlProductUrlFromJson(JSONObject product, String pid) {
      StringBuilder productUrl = new StringBuilder();

      if (pid != null && product.has("url")) {
         productUrl.append("https://www.fastshop.com.br/web/p/d/");
         productUrl.append(pid + "/");
         productUrl.append(CommonMethods.getLast(product.get("url").toString().split("/")));
      } else {
         return null;
      }

      return productUrl.toString();
   }

   private String crawlImgUrl(JSONObject product) {
      String imgUrlUnformatted = JSONUtils.getValueRecursive(product, "images.default", String.class);

      return "https:" + imgUrlUnformatted;
   }
}
