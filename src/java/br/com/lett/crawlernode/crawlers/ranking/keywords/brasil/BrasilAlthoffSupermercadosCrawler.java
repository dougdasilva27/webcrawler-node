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

public class BrasilAlthoffSupermercadosCrawler extends CrawlerRankingKeywords {
   public BrasilAlthoffSupermercadosCrawler(Session session) {
      super(session);
   }

   private String endCursor = null;
   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   protected JSONObject fetchDocument() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");

      String payload = "{\"accountId\":50,\"storeId\":" + getStoreId() + ",\"categoryName\":null,\"first\":12,\"promotion\":null,\"after\":"
         + endCursor + ",\"search\":\"" + this.keywordWithoutAccents + "\",\"brands\":[],\"categories\":[],\"tags\":[],\"personas\":[],\"sort\":" +
         "{\"field\":\"_score\",\"order\":\"desc\"},\"pricingRange\":{},\"highlightEnabled\":false}";

      Request request = Request.RequestBuilder.create()
         .setUrl("https://search.osuper.com.br/ecommerce_products_production/_search")
         .setPayload(payload)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
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
      this.totalProducts = JSONUtils.getIntegerValueFromJSON(json, "count", 0);

      if (json != null && !json.isEmpty()) {
         endCursor = "\"" + JSONUtils.getValueRecursive(json, "pageInfo.endCursor", String.class) + "\"";
         JSONArray productsArray = JSONUtils.getJSONArrayValue(json, "edges");

         for (Object product : productsArray) {
            JSONObject productJson = (JSONObject) product;
            String internalId = JSONUtils.getValueRecursive(productJson, "node.objectID", String.class);
            String productUrl = assemblyURL(productJson, internalId);
            String name = JSONUtils.getValueRecursive(productJson, "node.name", String.class);
            String imageUrl = JSONUtils.getValueRecursive(productJson, "node.image", String.class);
            boolean isAvailable = getAvaialability(productJson);
            Integer price = getPrice(productJson, isAvailable);

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
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private boolean getAvaialability(JSONObject productJson) {
      Integer stockInt = JSONUtils.getValueRecursive(productJson, "node.quantity.0.inStock", Integer.class);
      Double stockDouble = JSONUtils.getValueRecursive(productJson, "node.quantity.0.inStock", Double.class);
      if (stockInt != null) {
         return stockInt > 0;
      } else if (stockDouble != null) {
         return stockDouble > 0;
      }
      return false;
   }

   private Integer getPrice(JSONObject productJson, boolean isAvailable) {
      Double priceDouble = JSONUtils.getValueRecursive(productJson, "node.pricing.0.promotionalPrice", Double.class);
      if (isAvailable) {
         return (int) Math.round(priceDouble * 100);
      }
      return null;
   }

   private String assemblyURL(JSONObject productJson, String internalId) {
      String slugName = JSONUtils.getValueRecursive(productJson, "node.slug", String.class);
      if (slugName != null) {
         return "https://emcasa.althoff.com.br/produtos/" + internalId + "/" + slugName;
      }
      return null;
   }
}
