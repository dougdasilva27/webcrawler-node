package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

public class SaopauloDrogasilCrawler extends CrawlerRankingKeywords {

   public SaopauloDrogasilCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 24;

      this.log("Página " + this.currentPage);

      // monta a url com a keyword e a página
      String url = "https://api-gateway-prod.drogasil.com.br/search/v1/store/DROGASIL/product/search/live?term=" + this.keywordEncoded + "&limit="
         + this.pageSize + "&offset=" + this.arrayProducts.size();

      this.log("Link onde são feitos os crawlers: " + url);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String response = this.dataFetcher.get(session,request).getBody();

      JSONObject gridInfo = CrawlerUtils.stringToJson(response);
      JSONArray products = JSONUtils.getValueRecursive(gridInfo, "results.products", JSONArray.class);

      if (!products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(gridInfo);
         }

         for (Object o : products) {

            if (o instanceof JSONObject) {
               JSONObject productInfo = (JSONObject) o;
               String internalId = productInfo.optString("sku");
               String productUrl = productInfo.optString("urlKey");

               saveDataProduct(internalId, null, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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

   protected void setTotalProducts(JSONObject gridinfo) {
      this.totalProducts = JSONUtils.getValueRecursive(gridinfo, "metadata.totalCount", Integer.class);
      this.log("Total da busca: " + this.totalProducts);
   }
}
