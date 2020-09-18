package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class TottusCrawler extends CrawlerRankingKeywords {

   protected String homePage;

   public TottusCrawler(Session session) {
      super(session);
   }


   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 15;
      this.log("Página " + this.currentPage);


      String url = "https://" + homePage + "/buscar?q=" + this.keywordWithoutAccents.replace(" ", "%20") + "&page=" + this.currentPage;

      this.currentDoc = fetchDocument(url);

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, true, false);
      JSONObject props = jsonInfo.optJSONObject("props");
      JSONObject pageProps = props.optJSONObject("pageProps");
      JSONObject products = pageProps.optJSONObject("products");
      JSONArray results = products.optJSONArray("results");


      if (!results.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts();
         }

         for (Object e : results) {

            JSONObject skuInfo = (JSONObject) e;

            String internalId = skuInfo.optString("sku");
            String productUrl = CrawlerUtils.completeUrl(skuInfo.optString("key"), "https://", homePage) + "/p/";

            saveDataProduct(internalId, null, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
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
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".searchQuery span", true, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

}
