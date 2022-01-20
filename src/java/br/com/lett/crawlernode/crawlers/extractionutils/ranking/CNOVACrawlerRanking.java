package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import java.util.Arrays;
import java.util.HashMap;

public class CNOVACrawlerRanking extends CrawlerRankingKeywords {

   public CNOVACrawlerRanking(Session session) {
      super(session);
   }

   protected String getApiKey() {
      return session.getOptions().optString("apiKey");
   }

   @Override
   public void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {
      this.pageSize = 100;
      JSONObject search = fetchProductsFromAPI();

      if(search != null && !search.isEmpty()) {
         JSONArray products = JSONUtils.getJSONArrayValue(search, "products");
         for(Object product : products) {
            if(product instanceof JSONObject) {
               JSONObject productJSON = (JSONObject) product;
               String internalPid = productJSON.optString("id", null);
               String productUrl = productJSON.optString("url").split("\\?")[0];
               String name = productJSON.optString("name");
               String imgUrl = productJSON.optQuery("/images/default").toString();
               JSONArray variations = productJSON.optJSONArray("skus");
               int price = JSONUtils.getIntegerValueFromJSON(productJSON, "price", 0);
               boolean isAvailable = productJSON.optString("status", "").equalsIgnoreCase("AVAILABLE");

               if(variations != null && !variations.isEmpty()) {
                  for(Object variation : variations) {
                     if(variation instanceof JSONObject) {
                        JSONObject variationJSON = (JSONObject) variation;
                        String sku = variationJSON.optString("sku");
                        String internalId = internalPid + "-" + sku;

                        RankingProduct productRanking = RankingProductBuilder.create()
                           .setUrl(productUrl)
                           .setInternalId(internalId)
                           .setInternalPid(internalPid)
                           .setImageUrl(imgUrl)
                           .setName(name)
                           .setPriceInCents(price)
                           .setAvailability(isAvailable)
                           .build();

                        saveDataProduct(productRanking);
                     }
                  }
               } else {
                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(productUrl)
                     .setInternalId(null)
                     .setInternalPid(internalPid)
                     .setImageUrl(imgUrl)
                     .setName(name)
                     .setPriceInCents(price)
                     .setAvailability(isAvailable)
                     .build();

                  saveDataProduct(productRanking);
               }

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

   private JSONObject fetchProductsFromAPI() {
      String url = "https://prd-api-partner.viavarejo.com.br/api/search"
         + "?resultsPerPage="
         + this.pageSize + "&terms="
         + this.keywordEncoded
         + "&apiKey=" + getApiKey()
         + "&page=" + this.currentPage;

      HashMap<String, String> headers = new HashMap<String, String>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-encoding", "gzip, deflate, br");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("cache-control", "no-cache");
      headers.put("pragma", "no-cache");
      headers.put("sec-fetch-dest", "document");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-user", "?1");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("ak_bmsc", "DBC817405614D3D751F263F55B55A1228A7AC44DAC2100000AD4C05F24FBCD05~plWrjWUWgdXlFj2JvlQi2GxrJoTrHhd3KXQ7P6HmMQRj0r2higFJ+DGPeDauPrO9Pi+RivzTJiZ+GmcnKdY3P+Tx1ymEReA+p2mZ44U1c/plxcRhABRroUwMpWVYMQGzQJEGt9se0Tf6wuGi4RLAvV7MTRGi7qYMyVpn9G3p7zEVrGbvRY0Em6A5ZhSk/65b/8xkQEwz2tHjQIxHhdB+ZAUPu3Dw/rr5Hxog53uGYgmAk=");

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setCookies(this.cookies)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .build())
         .setHeaders(headers)
         .setProxyservice(Arrays.asList("netnut_residential_br_haproxy", "netnut_residential_co_haproxy", "netnut_residential_ar_haproxy"))
         .build();

      Response response = alternativeFetch(request);
      return JSONUtils.stringToJson(response.getBody());
   }

   private Response alternativeFetch(Request request) {
      Response response = new JsoupDataFetcher().get(session, request);
      Integer statusCode = response.getLastStatusCode();

      if (statusCode.toString().charAt(0) != '2' && statusCode.toString().charAt(0) != '3' && statusCode != 404) {
         response = new ApacheDataFetcher().get(session, request);
      }

      return response;
   }
}
