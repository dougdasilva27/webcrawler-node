package br.com.lett.crawlernode.crawlers.ranking.keywords.espana;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EspanaAtidaMifarmaCrawler extends CrawlerRankingKeywords {

   public static final String PRODUCTS_API_URL = "https://pyyto6qh8h-2.algolianet.com/1/indexes/*/queries?x-algolia-agent=Algolia%20for%20JavaScript%20(4.14.2)%3B%20Browser%20(lite)%3B%20JS%20Helper%20(3.11.0)%3B%20react%20(18.2.0)%3B%20react-instantsearch%20(6.31.1)&x-algolia-api-key=cf0cb3d62442678f9252636c8d47492d&x-algolia-application-id=PYYTO6QH8H";

   public EspanaAtidaMifarmaCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 50;
      this.log("Página " + this.currentPage);

      JSONObject search = fetchProductsFromAPI();

      JSONArray arraySkus = search != null ? JSONUtils.getValueRecursive(search, "results.0.hits", JSONArray.class) : new JSONArray();

      if (arraySkus.length() > 0) {

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (Object product : arraySkus) {
            JSONObject jsonSku = (JSONObject) product;
            String internalId = jsonSku.optString("sku");
            String name = jsonSku.optString("name");
            String productUrl = crawlProductUrl(jsonSku, internalId);

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(internalId)
               .setImageUrl(null)
               .setName(name)
               .setPriceInCents(null)
               .setAvailability(false)
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

   private void setTotalProducts(JSONObject search) {
      if (search.has("nbHits")) {
         this.totalProducts = search.getInt("nbHits");
      }
   }

   private String crawlProductUrl(JSONObject product, String internalId) {
      String slug = JSONUtils.getValueRecursive(product, "attributes.url_slug_es_es", String.class);
      String url = null;

      if (slug != null) {
         url = "https://www.atida.com/es-es/" + slug.replace("&", "");
      }

      return url;
   }


   private JSONObject fetchProductsFromAPI() {
      JSONObject products = new JSONObject();

      String payload = "{\"requests\":[{\"indexName\":\"product-ecommerce-es-es_es\",\"params\":\"analyticsTags=%5B%22type_searchresults%22%5D&clickAnalytics=true&facets=%5B%5D&getRankingInfo=true&highlightPostTag=%3C%2Fais-highlight-0000000000%3E&highlightPreTag=%3Cais-highlight-0000000000%3E&hitsPerPage=50&page=1&query=" + this.keywordEncoded + "&ruleContexts=%5B%22type_searchresults%22%2C%22dev_mobile%22%5D&tagFilters=&userToken=503b3c47-3bd6-4b9e-8e29-0e56eec7c91a\"}]}";
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Accept-Encoding", "no");

      Request request = Request.RequestBuilder.create().setUrl(PRODUCTS_API_URL).setCookies(cookies).setHeaders(headers).setPayload(payload)
         .mustSendContentEncoding(false).build();
      String page = this.dataFetcher.post(session, request).getBody();

      if (page.startsWith("{") && page.endsWith("}")) {
         try {

            // Using google JsonObject to get a JSONObject because this json can have a duplicate key.
            products = new JSONObject(new JsonParser().parse(page).getAsJsonObject().toString());

         } catch (Exception e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         }
      }

      return products;
   }
}
