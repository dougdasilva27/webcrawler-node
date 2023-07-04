package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.JSONUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BrasilMateusmaisCrawler extends CrawlerRankingKeywords {
   public BrasilMateusmaisCrawler(Session session) {
      super(session);
   }

   private String marketCode = session.getOptions().optString("marketId");


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);
      JSONObject responseJson = getProductList();

      if (!responseJson.isEmpty()) {
         this.totalProducts = responseJson.optInt("nbHits");
         if (this.totalProducts == 0) {

         }
         JSONArray productsList = responseJson.getJSONArray("hits");
         if (productsList != null && !productsList.isEmpty()) {

            for (Object productObject : productsList) {
               if (productObject instanceof JSONObject) {
                  JSONObject product = (JSONObject) productObject;
                  String internalId = product.optString("sku");
                  String productUrl = "https://mateusmais.com.br/#/produto/" + marketCode + "/" + product.optString("objectID");
                  String name = product.optString("name") + " " + product.optString("measure") + product.optString("measure_type");
                  String imageUrl = product.optString("image");
                  Integer price = crawlPrice(product);
                  Integer stock = product.optInt("amount_in_stock");
                  boolean isAvailable = stock > 0 ? true : false;
                  if (!isAvailable) {
                     price = null;
                  }

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(internalId)
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

   private Integer crawlPrice(JSONObject productsList) {
      Integer price;
      if (productsList.isNull("low_price")) {
         price = JSONUtils.getPriceInCents(productsList, "price");
      } else {
         price = JSONUtils.getPriceInCents(productsList, "low_price");
      }
      return price;
   }

   private JSONObject getProductList() {
      String url = "https://7drsoytyrm-dsn.algolia.net/1/indexes/SHOWCASE_catalog_product_api_index_PROD/query";
      String payload = "{\"params\":\"page=" + (this.currentPage - 1) + "&hitsPerPage=25&clickAnalytics=true&query=" + this.keywordEncoded + "&facetFilters=%5B%5B%22market_id%3A911b326e-89b3-4e39-a5e0-e6fd89e6040a%22%5D%2C%5B%5D%2C%5B%5D%5D&numericFilters=%5B%5D\"}";

      try {
         HttpClient client = HttpClient.newBuilder().build();
         HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .uri(URI.create(url))
            .header("X-Algolia-API-Key", "94f3c9f101d1af973f223496d3fa619e")
            .header("X-Algolia-Application-Id", "7DRSOYTYRM")
            .header(HttpHeaders.ACCEPT, "application/json, text/plain, */*")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .build();

         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
         return JSONUtils.stringToJson(response.body());
      } catch (Exception e) {
         throw new RuntimeException("Failed In load document: " + session.getOriginalURL(), e);
      }
   }

}
