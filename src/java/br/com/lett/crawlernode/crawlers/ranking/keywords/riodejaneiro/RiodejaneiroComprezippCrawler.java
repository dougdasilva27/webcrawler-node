package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;




public class RiodejaneiroComprezippCrawler extends CrawlerRankingKeywords {


   public RiodejaneiroComprezippCrawler(Session session) {
      super(session);
   }

   public JSONObject crawlApi() {
      int page = this.currentPage - 1;
      String apiUrl = "https://search.comprezipp.com/search?query=" + this.keywordEncoded + "&page=" + page;

      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", session.getOriginalURL());

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setHeaders(headers)
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

      JSONArray productsArray = JSONUtils.getValueRecursive(json, "data", JSONArray.class);

      if (productsArray != null && !productsArray.isEmpty()) {

         for (Object e : productsArray) {

            JSONObject product = (JSONObject) e;
//https://www.comprezipp.com/produto/frango-assado-sadia-batata-arroz-350g
               String internalId = JSONUtils.getIntegerValueFromJSON(product, "id", 0).toString();
               String slug = JSONUtils.getStringValue(product, "slug");

               String urlProduct = "";

               if (slug != null) {
                  urlProduct = "https://www.comprezipp.com/produto/" + slug;
               }

               saveDataProduct(internalId, null, urlProduct);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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

   @Override
   protected boolean hasNextPage() {

      return true;
   }

   protected void setTotalProducts(JSONObject json) {
      String totalProduct = "total";
      if (json.has(totalProduct) && json.get(totalProduct) instanceof Integer) {
         this.totalProducts = CrawlerUtils.getIntegerValueFromJSON(json, totalProduct, 0);
         this.log("Total da busca: " + this.totalProducts);
      }


}}

