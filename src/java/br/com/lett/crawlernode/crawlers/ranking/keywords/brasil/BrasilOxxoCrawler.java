package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BrasilOxxoCrawler extends CrawlerRankingKeywords {

   private String hostName = "https://www.oxxo.com.br";
   private String locationId = this.session.getOptions().optString("locationId", "1094871");

   public BrasilOxxoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);
      JSONObject responseJson = getProductList();

      if (!responseJson.isEmpty()) {

         if (this.totalProducts == 0) {
            this.totalProducts = JSONUtils.getValueRecursive(responseJson, "results.totalNumRecs", Integer.class, 0);
         }
         JSONArray productsList = JSONUtils.getValueRecursive(responseJson, "results.records", JSONArray.class, new JSONArray());
         if (productsList != null && !productsList.isEmpty()) {

            for (Object productObject : productsList) {
               if (productObject instanceof JSONObject) {
                  JSONObject product = (JSONObject) productObject;

                  String internalId = getSkuInfo(product, "product.repositoryId");
                  String productUrl = hostName + getSkuInfo(product, "product.route");
                  String name = getSkuInfo(product, "product.displayName");
                  String imageUrl = hostName + getSkuInfo(product, "product.primaryFullImageURL");
                  Integer price = CommonMethods.stringPriceToIntegerPrice(getSkuInfo(product, "sku.minActivePrice"), '.', null);
                  boolean isAvailable = price != null ? true : false;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
                     .setInternalPid(internalId)
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
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private String getSkuInfo(JSONObject productJson, String key) {
      JSONArray skuInfo = productJson.optJSONObject("attributes").optJSONArray(key);
      try {
         return skuInfo != null ? skuInfo.optString(0) : null;
      } catch (Exception e) {
         throw new RuntimeException("Not found information" + key, e);
      }
   }

   private JSONObject getProductList() {
      String url = getPageUrl();

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return JSONUtils.stringToJson(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

   private String getPageUrl() {
      int numberPage = currentPage - 1;
      int pagination = this.pageSize * numberPage;

      return "https://www.oxxo.com.br/ccstore/v1/assembler/pages/Default/osf/catalog?Nf=sku.activePrice%7CGT+0.0&No=" + pagination + "&Nr=AND%28sku.availabilityStatus%3AINSTOCK%2Csku.gn_sellerId%3A" + locationId + "%29&Nrpp=16&Ntt=" + this.keywordEncoded;
   }
}
