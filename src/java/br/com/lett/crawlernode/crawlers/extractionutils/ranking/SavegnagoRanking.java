package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class SavegnagoRanking extends CrawlerRankingKeywords {

   private static final int SALES_CHANNEL = 1;
   private static final String BASE_URL = "www.savegnago.com.br";
   private static final String API_KEY = "savegnago";

   public SavegnagoRanking(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 32;
      this.log("Página " + this.currentPage);

      String url = "https://www.savegnago.com.br/" + this.keywordEncoded;
      this.currentDoc = fetchDocument(url);

      if (currentDoc.selectFirst(".product-card") != null) {
         //Get from the html
         this.log("Link onde são feitos os crawlers: " + url);
         Elements products = this.currentDoc.select(".n4colunas li[layout]");

         if (products != null && !products.isEmpty()) {
            if (totalProducts == 0) {
               setTotalProducts();
            }
            for (Element product : products) {
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".product-card", "item");
               String internalPid = internalId;
               String productUrl = CrawlerUtils.scrapUrl(product, ".prod-acc > a", "href", "https", BASE_URL);
               saveDataProduct(internalId, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         } else {
            result = false;
            log("Keyword sem resultado!");
         }
      } else {
         //Get from API
         JSONObject json = loadJson();
         JSONArray products = json.optJSONArray("products");

         if (products != null && !products.isEmpty()) {
            if (totalProducts == 0) {
               setTotalProducts(json);
            }
            for (Object productObj : products) {
               JSONObject product = (JSONObject) productObj;
               String internalId = product.optString("id");
               String internalPid = internalId;
               String productAPIUrl = product.optString("url").replace("//", "");
               String productUrl = CrawlerUtils.completeUrl(productAPIUrl,"https",BASE_URL);
               saveDataProduct(internalId, internalPid, productUrl);

               log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
               if (arrayProducts.size() == productsLimit) {
                  break;
               }
            }
         } else {
            result = false;
            log("Keyword sem resultado!");
         }
      }
      log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados");
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".resultado-busca-numero .value", true, 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("size", 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   private JSONObject loadJson() {
      String url = "https://api.linximpulse.com/engage/search/v3/search/?salesChannel=" + SALES_CHANNEL + "&terms=" + this.keywordEncoded + "&resultsPerPage=32&page=" + this.currentPage + "&apiKey=" + API_KEY;

      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.savegnago.com.br");
      headers.put("content-type:", "application/json");

      Request request = Request.RequestBuilder.create().setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .mustSendContentEncoding(false)
         .build();
      Response response = this.dataFetcher.get(session, request);
      return CrawlerUtils.stringToJson(response.getBody());
   }
}
