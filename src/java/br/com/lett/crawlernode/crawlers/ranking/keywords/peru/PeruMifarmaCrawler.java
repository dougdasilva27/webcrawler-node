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
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY)
         )
         .setSendUserAgent(false)
         .build();

      String json = new JsoupDataFetcher().post(session, request).getBody();
      return JSONUtils.stringToJson(json);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {

      this.log("Página " + this.currentPage);

      String url = "https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/MFPRD/filtered-products";
      String payload = getPayload();

      JSONObject json = fetchJSONObject(url, payload);
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

   private Integer getPriceIfAvailable(JSONObject product) {
      Integer priceInCents = null;
      String status = product.optString("productStatus");
      if ("AVAILABLE".equals(status)) {
         Double price = JSONUtils.getDoubleValueFromJSON(product, "priceAllPaymentMethod", true);
         if (price == null || price == 0.0) {
            price = product.optDouble("price");
         }
         priceInCents = CommonMethods.doublePriceToIntegerPrice(price, null);
      }
      return priceInCents;
   }

   private String scrapUrl(String slug, String internalPid) {
      if (slug != null && !slug.isEmpty()) {
         return "https://mifarma.com.pe/producto/" + slug + "/" + internalPid;
      }
      return null;
   }

   private String getPayload() {
      JSONObject payloadJson = new JSONObject();
      List<String> ids = new ArrayList<>();

      String payload = "{\"query\":\"" + keywordEncoded + "\"}";
      String url = "https://5doa19p9r7.execute-api.us-east-1.amazonaws.com/MFPRD/search-filters";

      JSONObject json = fetchJSONObject(url, payload);
      JSONArray products = json.optJSONArray("productsId");
      if (products != null && !products.isEmpty()) {
         for (Object obj : products) {
            if (obj instanceof String) {
               ids.add(obj.toString());
            }
         }

         payloadJson.put("rows", products.length());
         payloadJson.put("order", "ASC");
         payloadJson.put("sort", "ranking");
         payloadJson.put("productsFilter", ids);

         return payloadJson.toString();
      }

      return "";
   }

}

//https://mifarma.com.pe/producto/antitranspirante-dermoaclarant-en-spray-dove-48-ho/011640
//https://mifarma.com.pe/product/antitranspirante-dermoaclarant-en-spray-dove-48-ho/011640
