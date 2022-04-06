package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BarbosaSupermercadosRanking extends CrawlerRankingKeywords {
   public BarbosaSupermercadosRanking(Session session) {
      super(session);
   }

   private String STORE_ID = session.getOptions().optString("storeId");
   private JSONObject scOptions = session.getOptions().optJSONObject("scraperClass");

   private JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Connection", "keep-alive");
      headers.put("referer", "https://www.barbosasupermercados.com.br/");
      headers.put("accept", "application/json, text/plain, */*");

      String appSecret = scOptions.optString("app_secret");
      String appKey = scOptions.optString("app_key");

      String url = "https://bsm.applayos.com:6033/api/ecom/enav/buscarprodutos";
      String payload = "{\"filter\":{\"text\":\"" + this.keywordWithoutAccents + "\"},\"session\":{\"loja\":{\"id\":\"" + STORE_ID + "\"}},\"limit\":350,\"app_key\":\"" + appKey + "\",\"app_secret\":\"" + appSecret + "\"}";

      headers.put("Content-Length", String.valueOf(payload.length()));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
//         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY))
         .setSendUserAgent(false)
         .build();

      try {
         Response response = new FetcherDataFetcher().post(session, request);
         String content = response.getBody();
         return CrawlerUtils.stringToJson(content);
      } catch (Exception e) {
         Logging.printLogError(logger, CommonMethods.getStackTrace(e));
      }

      return new JSONObject();
   }


   @Override
   public void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 350;
      this.log("Página " + this.currentPage);

      JSONObject data = fetch();
      JSONArray items = JSONUtils.getValueRecursive(data, "data.produtos", JSONArray.class, new JSONArray());
      if (!items.isEmpty()) {
         for (Object o : items) {
            if (o instanceof JSONObject) {
               JSONObject product = (JSONObject) o;

               String internalId = product.optString("id");
               String internalPid = product.optString("sku");
               String productUrl = CrawlerUtils.completeUrl(product.optString("url_key") + product.optString("url_suffix"), "https", "naturaldaterra.com.br");
               String name = product.optString("name");
               String imageUrl = JSONUtils.getValueRecursive(product, "small_image.url", String.class);
               int price = 0;
               boolean isAvailable = price != 0;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPriceInCents(price)
                  .setAvailability(isAvailable)
                  .setImageUrl(imageUrl)
                  .build();

               saveDataProduct(productRanking);

               if (this.arrayProducts.size() == productsLimit)
                  break;
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }
}
