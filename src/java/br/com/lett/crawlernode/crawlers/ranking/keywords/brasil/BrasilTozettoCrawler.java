package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

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

import java.util.HashMap;
import java.util.Map;

public class BrasilTozettoCrawler extends CrawlerRankingKeywords {
   public BrasilTozettoCrawler(Session session) {
      super(session);
   }

   private final String storeId = session.getOptions().optString("storeId");
   protected JSONObject fetchDocument() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      String payload = "{\"accountId\":58,\"storeId\":"+storeId+",\"categoryName\":null,\"first\":1000,\"promotion\":null,\"after\":null,\"search\":\""+this.keywordWithoutAccents+"\",\"brands\":[],\"categories\":[],\"tags\":[],\"personas\":[],\"sort\":{\"field\":\"_score\",\"order\":\"desc\"},\"pricingRange\":{},\"highlightEnabled\":true}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://search.osuper.com.br/ecommerce_products_production/_search")
         .setPayload(payload)
         .setHeaders(headers)
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
         JSONArray productsArray = JSONUtils.getJSONArrayValue(json, "edges");
         this.totalProducts = productsArray.length();

         for (Object product : productsArray) {
            JSONObject productObj = (JSONObject) product;
            JSONObject productJson = JSONUtils.getValueRecursive(productObj, "node", JSONObject.class, new JSONObject());;

            if(productJson != null) {
               String internalId = JSONUtils.getStringValue(productJson, "objectID");
               String productUrl = "https://online.tozetto.com.br/produtos/" + internalId + "/" +JSONUtils.getStringValue(productJson, "slug");
               String name = JSONUtils.getStringValue(productJson, "name");
               String imageUrl = JSONUtils.getStringValue(productJson, "image");
               Double price = JSONUtils.getValueRecursive(productJson, "pricing.0.promotionalPrice", Double.class);
               Integer priceInCents = price != null ? (int) Math.round((Double) price * 100) : JSONUtils.getValueRecursive(productJson, "pricing.0.price", Integer.class);
               Integer stock = JSONUtils.getValueRecursive(productJson, "quantity.0.inStock", Integer.class);
               boolean isAvailable = stock != null && stock != 0;

               RankingProduct productRanking = RankingProductBuilder.create()
                  .setUrl(productUrl)
                  .setInternalId(internalId)
                  .setName(name)
                  .setPriceInCents(priceInCents)
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
