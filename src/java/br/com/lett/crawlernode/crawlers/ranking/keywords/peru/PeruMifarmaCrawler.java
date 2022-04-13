package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class PeruMifarmaCrawler extends CrawlerRankingKeywords {

   protected int totalPages;

   public PeruMifarmaCrawler(Session session) {
      super(session);
   }

   protected JSONObject fetchJSONObject(String url, String payload) {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY)
         )
         .build();

      String json = new JsoupDataFetcher().post(session, request).getBody();
      return JSONUtils.stringToJson(json);
   }


   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);

      String payload = getPayload();

      String url = "https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/MFPRD/filtered-products";

      JSONObject json = fetchJSONObject(url, payload);
      JSONArray products = json.optJSONArray("rows");

      if (products != null && !products.isEmpty()) {
         if (totalProducts == 0) {
            this.totalPages = json.optInt("records");
            setTotalProducts(json);
         }

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalPid = product.optString("id");
               String productUrl = "https://mifarma.com.pe/producto/" + product.optString("uri") + "/" + internalPid;
               String name = product.optString("name");
               String imgUrl = scrapImage(internalPid);
               Integer price = getPriceIfAvailable(product);
               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(null)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setImageUrl(imgUrl)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .build();

               saveDataProduct(productRanking);

               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return this.currentPage < totalPages;
   }

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("nbHits");
      this.log("Total de produtos: " + this.totalProducts);
   }

   private String scrapImage(String internalPid) {
      String imgUrl = "https://dcuk1cxrnzjkh.cloudfront.net/imagesproducto/" + internalPid + "X.jpg";

      return imgUrl;
   }

   private Integer getPriceIfAvailable(JSONObject product) {
      String status = product.optString("productStatus");
      if ("AVAILABLE".equals(status)) {
         return CommonMethods.doublePriceToIntegerPrice(product.optDouble("price"), null);
      }
      return null;
   }


   private String getPayload() {
      List<String> ids = new ArrayList<>();
      String url = "https://o74e6qkj1f-dsn.algolia.net/1/indexes/" +
         "products/query?x-algolia-agent=Algolia%20for%20JavaScript%20(3.35.1)%3B%20Browser" +
         "&x-algolia-application-id=O74E6QKJ1F&x-algolia-api-key=b65e33077a0664869c7f2544d5f1e332";



      JSONObject json = fetchJSONObject(url, payload);
      JSONArray products = json.optJSONArray("hits");
      for (Object obj : products) {
         if (obj instanceof JSONObject) {
            JSONObject product = (JSONObject) obj;
            String internalPid = product.optString("objectID");
            if (internalPid != null && !internalPid.isEmpty()) ids.add(internalPid);
         }
      }

      String a = "{\"departmentsFilter\":[],\"categoriesFilter\":[],\"subcategoriesFilter\":[],\"brandsFilter\":[],\"page\":0,\"rows\":8,\"order\":\"ASC\",\"sort\":\"ranking\",\"productsFilter\": " + ids.toString() + "}";
      return "{\"departmentsFilter\":[],\"categoriesFilter\":[],\"subcategoriesFilter\":[],\"brandsFilter\":[],\"page\":0,\"rows\":8,\"order\":\"ASC\",\"sort\":\"ranking\",\"productsFilter\": " + ids.toString() + "}";

   }


}
