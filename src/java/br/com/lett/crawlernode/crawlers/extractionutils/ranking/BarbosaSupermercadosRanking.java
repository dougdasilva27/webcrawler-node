package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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

import java.util.HashMap;
import java.util.Map;

public class BarbosaSupermercadosRanking extends CrawlerRankingKeywords {
   public BarbosaSupermercadosRanking(Session session) {
      super(session);
   }

   private final String STORE_ID = session.getOptions().optString("storeId");
   private final JSONObject scOptions = session.getOptions().optJSONObject("scraperClass");

   private JSONObject fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Connection", "keep-alive");
      headers.put("referer", "https://www.barbosasupermercados.com.br/");

      String appSecret = scOptions.optString("app_secret");
      String appKey = scOptions.optString("app_key");

      String url = "https://bsm.applayos.com:6033/api/ecom/enav/buscarprodutos";
      String payload = "{\"filter\":{\"text\":\"" + this.keywordWithoutAccents + "\"},\"session\":{\"loja\":{\"id\":\"" + STORE_ID + "\"}},\"limit\":350,\"app_key\":\"" + appKey + "\",\"app_secret\":\"" + appSecret + "\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .mustSendContentEncoding(false)
         .build();

      int tries = 0;
      Response response = null;

      while (tries < 3) {
         try {
            response = new FetcherDataFetcher().post(session, request);
            String content = response.getBody();
            if (response.isSuccess()) {
               return CrawlerUtils.stringToJson(content);
            }
         } catch (Exception e) {
            Logging.printLogError(logger, CommonMethods.getStackTrace(e));
         }
         tries++;
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

               String internalId = product.optString("sku");
               String internalPid = internalId;
               String productUrl = scrapUrl(product);
               String name = product.optString("descricao");
               String imageUrl = JSONUtils.getValueRecursive(product, "files2.0.url", String.class);
               Integer price = JSONUtils.getPriceInCents(product, "por", Integer.class, null);
               boolean isAvailable = price != null;

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

   private String scrapUrl(JSONObject product) {
      String url = "https://www.barbosasupermercados.com.br/#product=";
      String productUri = product.optString("uri", "");
      if (!productUri.isEmpty()) {
         return url + productUri;
      }
      return null;
   }

   @Override
   protected boolean hasNextPage() {
      return false;
   }
}
