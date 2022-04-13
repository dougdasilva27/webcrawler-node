package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
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

   protected JSONObject fetchJSONObject(String url, String payload, DataFetcher dataFetcher) {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("origin", "https://mifarma.com.pe");
      headers.put("referer", "https://mifarma.com.pe/");
      headers.put("authority", "5doa19p9r7.execute-api.us-east-1.amazonaws.com");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY)
         )
         .build();

      String json = dataFetcher.post(session, request).getBody();
      return JSONUtils.stringToJson(json);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 10;
      this.log("Página " + this.currentPage);

      String payload = getPayload();

      String url = "https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/MFPRD/filtered-products";

      JSONObject json = fetchJSONObject(url, payload, new JsoupDataFetcher());
      JSONArray products = json.optJSONArray("rows");

      if (products != null && !products.isEmpty()) {

         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalPid = product.optString("id");
               String productUrl = scrapUrl(product.optString("slug"), internalPid);
               String name = product.optString("name");
               String imgUrl = JSONUtils.getValueRecursive(product, "imageList.0.url", String.class);
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
      return true;
   }

   private Integer getPriceIfAvailable(JSONObject product) {
      String status = product.optString("productStatus");
      if ("AVAILABLE".equals(status)) {
         return CommonMethods.doublePriceToIntegerPrice(product.optDouble("price"), null);
      }
      return null;
   }

   private String scrapUrl(String slug, String internalPid) {
      if (slug != null && !slug.isEmpty()) {
         return "https://mifarma.com.pe/product/" + slug + "/" + internalPid;
      }
      return null;
   }

   private String getPayload() {
      JSONObject payloadJson = new JSONObject();
      List<String> ids = new ArrayList<>();

      String url = "https://o74e6qkj1f-dsn.algolia.net/1/indexes/" +
         "products/query?x-algolia-agent=Algolia%20for%20JavaScript%20(3.35.1)%3B%20Browser" +
         "&x-algolia-application-id=O74E6QKJ1F&x-algolia-api-key=b65e33077a0664869c7f2544d5f1e332";

      String payload = "{\"params\":\"query=" + this.keywordEncoded + "&attributesToRetrieve=%5B%22objectID%22%2C%22name%22%2C%22uri%22%5D&hitsPerPage=10&page=" + (this.currentPage - 1) + "\"}";

      JSONObject json = fetchJSONObject(url, payload, new JsoupDataFetcher());
      JSONArray products = json.optJSONArray("hits");
      if (products != null && !products.isEmpty()) {
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;
               String internalPid = product.optString("objectID");
               if (internalPid != null && !internalPid.isEmpty()) ids.add(internalPid);
            }
         }
         System.out.println("Workkk");
         payloadJson.put("page", this.currentPage - 1);
         payloadJson.put("rows", products.length());
         payloadJson.put("order", "ASC");
         payloadJson.put("sort", "ranking");
         payloadJson.put("productsFilter", ids);

         return payloadJson.toString();
      }

      return "{\"page\":0,\"rows\":10,\"order\":\"ASC\",\"sort\":\"ranking\",\"productsFilter\": [\"029136\",\"011640\",\"424079\",\"430683\",\"010278\",\"424080\",\"003408\",\"431498\",\"003411\",\"023871\"]}";

   }


}
