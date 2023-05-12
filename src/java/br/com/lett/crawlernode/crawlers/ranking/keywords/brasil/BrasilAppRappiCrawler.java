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

   protected JSONObject fetchDocument() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("authorization", "Bearer ft.gAAAAABkXTUqW4WZs3GQ4TwL7iXdRd_6_So4YqGDeJTbK_pLq25-mj1SYmoVpEcqC2FiRufXyE8VA265rUbYNUTK_hn7TV6Su13OyZEXjobm9Dxs1VshF1tbUPEFW_H0qhre7e4YnnZ5j1wamsRb228mzuAz2PCdjit0576uNDTGDJd-vXET3g0Qe1h_d86gIk6fFCmiHRA0fdHQ-jjS3DndmVFBo36_H4-HKISZ9Pohe1NcOPgr-CvO6cqJWj2zp6qqpENxwA2rbFpfzx2L4ESo-kVOk8Adpgr2sAM5WKW8etCVrhBMcbZVE75pSZHifxXIyMDITaxhdgDzWtRMlmlHvo5KVAZcrH_CoCeTTGHZ0Bo7NHzk9GI=");

      String payload = "{\"query\":" +this.keywordWithoutAccents+ ",\"lat\":-27.57929014867,\"lng\":-48.588943853974,\"options\":{\"sort\":{}},\"tiered_stores\":[\"default\",\"standard\",\"premium\"]}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://services.rappi.com.br/api/pns-global-search-api/v1/unified-search?is_prime=false&unlimited_shipping=false")
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);

      return JSONUtils.stringToJson(response.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchDocument();

      if (json != null && !json.isEmpty()) {
         JSONArray productsArray = JSONUtils.getJSONArrayValue(json, "products");
         this.totalProducts = productsArray.length();

         for (Object product : productsArray) {
            JSONObject productJson = (JSONObject) product;

            if(productJson != null) {
               String internalId = Integer.toString(JSONUtils.getIntegerValueFromJSON(productJson, "master_product_id", 0));
               //String productUrl = JSONUtils.getStringValue(productJson, "url");
               String name = JSONUtils.getStringValue(productJson, "name");
               String imageUrl = JSONUtils.getStringValue(productJson, "image");
               Integer price = JSONUtils.getPriceInCents(productJson, "price");
               boolean isAvailable = price != null;

               RankingProduct productRanking = RankingProductBuilder.create()
                  //.setUrl(productUrl)
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
