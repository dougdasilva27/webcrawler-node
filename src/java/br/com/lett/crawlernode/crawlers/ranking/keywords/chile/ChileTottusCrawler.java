package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class ChileTottusCrawler extends CrawlerRankingKeywords {

   public ChileTottusCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);

      String url = "https://www.tottus.cl/buscar?q=" + this.keywordWithoutAccents + "&page=" + this.currentPage;
      System.out.println(url);
      this.currentDoc = fetchDocument(url);

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, true, false);
      JSONObject props = JSONUtils.getJSONValue(jsonInfo, "props");
      JSONObject pageProps = JSONUtils.getJSONValue(props, "pageProps");
      JSONObject products = JSONUtils.getJSONValue(pageProps, "products");
      JSONArray results = JSONUtils.getJSONArrayValue(products, "results");


      if (!results.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         if (results != null) {

            for (Object e : results) {

               JSONObject skuInfo = (JSONObject) e;

               String internalId = skuInfo.optString("sku");
               String productUrl = CrawlerUtils.completeUrl(skuInfo.optString("key"), "https://", "www.tottus.cl") + "/p/";

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

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".searchQuery span", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
