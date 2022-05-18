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
            if(productObject instanceof JSONObject){
               JSONObject product = (JSONObject) productObject;
               String internalId = product.optString("id");
               String productUrl = crawurl(product);
               String name = crawmame(product);
               String imageUrl = crawimg(product);
               Integer price = null;
               boolean isAvailable = crawisavailable(product);
               if (isAvailable == true) {
                  crawprice(productsList);
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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   private boolean crawisavailable(JSONArray productsList) {
      String holder;
      Object obj = productsList.optQuery("available");
      if (obj != null) {
         holder = obj.toString();
         if (holder.contains("true")) {
            return true;
         }
      }
      return false;
   }

   private Integer crawprice(JSONArray productsList) {
      Integer numb = null;
      Object obj = productsList.optQuery("low_price");
      if (obj != null) {
         obj = productsList.optQuery("price");
         numb = Integer.valueOf(obj.toString());
      } else {
         numb = Integer.valueOf(obj.toString());
      }
      return numb;
   }

   private String crawimg(JSONArray productsList) {
      Object obj = productsList.optQuery("image");
      if (obj != null) {
         return obj.toString();
      }
      return null;
   }

   private String crawmame(JSONArray productsList) {
      Object obj = productsList.optQuery("name");
      if (obj != null) {
         return obj.toString();
      }
      return null;
   }

   private String crawurl(String internalId) {
      return "https://mateusmais.com.br/produto/2857c51e-ffc9-4365-b39a-0156cfc032b9/" + internalId;
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

   @Override
   protected boolean hasNextPage() {
      Element page = this.currentDoc.selectFirst("#os-content > div > div > span > span > ul > li");
      return page != null;
   }
}
