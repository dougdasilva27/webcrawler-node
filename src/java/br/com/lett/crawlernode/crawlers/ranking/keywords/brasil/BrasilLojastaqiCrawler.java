package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilLojastaqiCrawler extends CrawlerRankingKeywords {


   public BrasilLojastaqiCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi() {

      String apiUrl = "https://www.taqi.com.br/ccstoreui/v1/search?Nrpp=" + productsLimit + "&totalResults=true&No=0&Ntt=" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", session.getOriginalURL());
      headers.put("content-type", "application/json; charset=UTF-8");

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
         .build();
      String content = new FetcherDataFetcher()
         .get(session, request)
         .getBody();

      if (content == null || content.isEmpty()) {
         content = this.dataFetcher.get(session, request).getBody();
      }

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      JSONObject json = crawlApi();
      JSONObject resultsList = JSONUtils.getJSONValue(json, "resultsList");

      JSONArray productsArray = JSONUtils.getValueRecursive(resultsList, "records", JSONArray.class);

      if (productsArray != null && productsArray.length() >= 1) {

         for (Object e : productsArray) {

            JSONObject product = (JSONObject) e;

            JSONObject attributes = JSONUtils.getValueRecursive(product, "records.0.attributes", JSONObject.class);
            if (attributes != null) {
               JSONArray internalIdArray = JSONUtils.getJSONArrayValue(attributes, "sku.repositoryId");
               String internalId = internalIdArray != null ? internalIdArray.getString(0) : null;
               JSONArray urlProductIncompleteArray = JSONUtils.getJSONArrayValue(attributes, "product.route");
               String urlProductIncomplete = urlProductIncompleteArray != null ? urlProductIncompleteArray.getString(0) : null;

               String urlProduct = "";

               if (urlProductIncomplete != null) {
                  urlProduct = "https://www.taqi.com.br" + urlProductIncomplete;
               }

               saveDataProduct(internalId, null, urlProduct);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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

   @Override
   protected boolean hasNextPage() {
      return false;
   }

   protected void setTotalProducts(JSONObject resultsList) {
      String totalProduct = "totalRecNum";
      if (resultsList.has(totalProduct) && resultsList.get(totalProduct) instanceof Integer) {
         this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(resultsList, totalProduct, 0);
         this.log("Total da busca: " + this.totalProducts);
      }
   }


}
