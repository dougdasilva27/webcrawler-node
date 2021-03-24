package br.com.lett.crawlernode.crawlers.ranking.keywords.saobernardodocampo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class SaobernardodocampoRoldaoatacadistaCrawler extends CrawlerRankingKeywords {

   //https://api.roldao.com.br/api/public/store/search?query=leite&page=2&order=relevance&save=true

   public SaobernardodocampoRoldaoatacadistaCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi() {
      String apiUrl = "https://api.roldao.com.br/api/public/store/search?query=" + this.keywordEncoded + "&page=" + this.currentPage + "&order=relevance&save=true";

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      return CrawlerUtils.stringToJson(content);

   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      JSONObject json = crawlApi();

      JSONArray productsArray = JSONUtils.getValueRecursive(json, "products", JSONArray.class);

      if (productsArray != null && !productsArray.isEmpty()) {
         if (totalProducts == 0)
            setTotalProducts(json);

         for (Object e : productsArray) {

            JSONObject product = (JSONObject) e;

            String internalId = product.optString("sku");
            String internalPid = product.optString("barcode");
            String token = product.optString("token");
            String urlProduct = token != null && !token.isEmpty() ? "https://www.roldao.com.br/product-details/" + token.replace(" ", "%20").toLowerCase() : null;

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
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

   protected void setTotalProducts(JSONObject json) {
      this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(json, "total_products", 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}

