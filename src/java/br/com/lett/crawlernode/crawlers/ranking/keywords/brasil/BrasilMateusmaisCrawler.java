package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class BrasilMateusmaisCrawler extends CrawlerRankingKeywords {
   public BrasilMateusmaisCrawler(Session session) {
      super(session);
   }

   String marketCode = session.getOptions().optString("marketId");


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 30;
      this.log("Página " + this.currentPage);
      JSONObject reponseJson = getProductList();


      if (!reponseJson.isEmpty()) {
         this.totalProducts = reponseJson.optInt("count");
         if (this.totalProducts == 0) {

         }

         JSONArray productsList = reponseJson.getJSONArray("results");

         for (Object productObject : productsList) {
            if (productObject instanceof JSONObject) {
               JSONObject product = (JSONObject) productObject;
               String internalId = product.optString("sku");
               String productUrl = "https://mateusmais.com.br/produto/" + marketCode + "/" + product.optString("id");
               String name = product.optString("name") + " " + product.optString("measure") + product.optString("measure_type");
               String imageUrl = product.optString("image");
               Integer price = null;
               boolean isAvailable = crawisavailable(product);
               if (isAvailable == true) {
                  price = crawlPrice(product);
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
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }
      hasNextPage(reponseJson);
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private boolean crawisavailable(JSONObject productsList) {

      String holder = productsList.optString("available");
      if (holder.contains("true")) {
         return true;
      }
      return false;
   }

   private Integer crawlPrice(JSONObject productsList) {
      Integer price = null;
      if (productsList.isNull("low_price")) {
         price = JSONUtils.getPriceInCents(productsList, "price");
      } else {
         price = JSONUtils.getPriceInCents(productsList, "low_price");
      }
      return price;
   }

   private String crawid(JSONObject productsList) {
      Object obj = productsList.optString("id");
      if (obj != null) {
         return obj.toString();
      }
      return null;
   }

   private JSONObject getProductList() {
      String url = "https://app.mateusmais.com.br/market/" + marketCode + "/product/?page=" + currentPage + "&market=" + marketCode + "&search=" + keywordEncoded;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         return CrawlerUtils.stringToJson(response.getBody());
      }
      return null;

   }

   protected boolean hasNextPage(JSONObject reponseJson) {
      String page = reponseJson.optString("next");
      return page != null;
   }
}
