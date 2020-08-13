package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class BrasilEssencialpravcCrawler extends CrawlerRankingKeywords {

   public BrasilEssencialpravcCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 16;
      this.log("Página " + this.currentPage);

      JSONArray products = fetchApi();

      if (products != null && !products.isEmpty()) {
         for (Object element : products) {
            JSONObject product = (JSONObject) element;

            String internalPid = product.optString("productId");
            String productUrl = product.optString("link");

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

   @Override
   protected boolean hasNextPage() {
      return (this.arrayProducts.size() / this.currentPage) >= this.pageSize;
   }

   private JSONArray fetchApi() {
      String url = "https://www.essencialpravc.com.br/api/catalog_system/pub/products/search//" + this.keywordWithoutAccents.replace(" ", "%20")
            + "?map=b&_from=" + this.arrayProducts.size() + "&_to=" + (this.arrayProducts.size() + 15);
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();

      Request request = Request.RequestBuilder.create()
            .setHeaders(headers)
            .setUrl(url)
            .build();

      String response = dataFetcher.get(session, request).getBody();
      return CrawlerUtils.stringToJsonArray(response);
   }
}
