package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;

public class BrasilMateusmaisCrawler extends CrawlerRankingKeywords {
   public BrasilMateusmaisCrawler(Session session) {
      super(session);
   }

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
               String productUrl = "https://mateusmais.com.br/produto/2857c51e-ffc9-4365-b39a-0156cfc032b9/" + internalId;
               String name = product.optString("name");
               String imageUrl = product.optString("image");
               Integer price = null;
               boolean isAvailable = crawisavailable(product);
               if (isAvailable == true) {
                  price = crawprice(product);
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

   private Integer crawprice(JSONObject productsList) {
      Integer numb = null;
      String obj = productsList.optString("low_price").replace(".", "");
      if (obj != null) {
         obj = productsList.optString("price").replace(".", "");
         numb = Integer.parseInt(obj);
      } else {
         numb = Integer.parseInt(obj);
      }
      return numb;
   }

   private String crawid(JSONObject productsList) {
      Object obj = productsList.optString("id");
      if (obj != null) {
         return obj.toString();
      }
      return null;
   }

   private JSONObject getProductList() {
      String url = "https://app.mateusmais.com.br/market/2857c51e-ffc9-4365-b39a-0156cfc032b9/product/?page=" + currentPage + "&market=2857c51e-ffc9-4365-b39a-0156cfc032b9&search=" + keywordEncoded;

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
