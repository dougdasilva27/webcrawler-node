package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BrasilAppRappiCrawler extends CrawlerRankingKeywords {
   public BrasilAppRappiCrawler(Session session) {
      super(session);
   }

   private final String storeId = session.getOptions().optString("storeId");
   private final String sizeLimit = session.getOptions().optString("sizeLimit");

   protected JSONObject fetchJSONObject() {
      String payload = "{\"query\":\"" + this.keywordWithoutAccents + "\",\"size\":" + sizeLimit + ",\"from\":0,\"vertical_group\":\"cpgs\",\"object_id\":\"\"}";
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("content-length", payload.length() + "");
      headers.put("host", "services.rappi.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://services.rappi.com.br/api/cpgs/search/v2/store/" + storeId + "/products")
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.BUY_HAPROXY))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchJSONObject();

      if (json != null && !json.isEmpty()) {
         JSONArray productsArray = JSONUtils.getJSONArrayValue(json, "products");
         this.totalProducts = productsArray.length();
         for (Object product : productsArray) {
            JSONObject productJson = (JSONObject) product;

            if (productJson != null) {
               String internalId = productJson.optString("id");
               String name = productJson.optString("name");
               String imageUrl = productJson.optString("image");
               Integer price = productJson.optInt("price");
               boolean isAvailable = productJson.optBoolean("in_stock");

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(session.getOptions().optString("preLink")+internalId)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);
            }
            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }


      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

}
