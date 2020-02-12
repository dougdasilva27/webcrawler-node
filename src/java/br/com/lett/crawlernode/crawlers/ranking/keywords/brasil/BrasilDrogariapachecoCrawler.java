package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilDrogariapachecoCrawler extends CrawlerRankingKeywords {

   public BrasilDrogariapachecoCrawler(Session session) {
      super(session);
   }

   private JSONArray extractJsonFromApi() {

      String url = "https://www.drogariaspacheco.com.br/api/catalog_system/pub/products/search/?ft=" + this.keywordWithoutAccents + "&_from=0&_to=8&O=OrderByReleaseDateDESC";

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36");
      headers.put("Accept", "*/*");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String body = this.dataFetcher.get(session, request).getBody();

      JSONArray stringToJson = JSONUtils.stringToJsonArray(body);

      return stringToJson;

   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 18;

      this.log("Página" + this.currentPage);

      JSONArray products = extractJsonFromApi();

      if (products.length() > 0) {
         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String productUrl = crawlProductUrl(product);
            String internalPid = crawlInternalPid(product);

            saveDataProduct(null, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + null + " - InternalPid: " + internalPid + " - Url: " + productUrl);


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

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("productId")) {
         internalPid = product.getString("productId");
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String urlProduct = null;

      if (product.has("link")) {
         urlProduct = product.getString("link");
      }

      return urlProduct;
   }
}
