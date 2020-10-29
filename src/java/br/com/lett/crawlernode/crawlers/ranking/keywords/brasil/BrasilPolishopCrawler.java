package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

public class BrasilPolishopCrawler extends CrawlerRankingKeywords {

   public BrasilPolishopCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      String key = this.keywordWithoutAccents.replaceAll(" ", "%20");

      String url = "https://www.polishop.com.br/" + key + "?_q=" + key + "&map=ft&page=" + this.currentPage;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);
      JSONArray productsJson = scrapProductsArray(currentDoc);

      if (!productsJson.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts();
         for (Object e : productsJson) {
            if (e instanceof JSONObject) {

               String productUrl = ((JSONObject) e).optString("url");
               saveDataProduct(null, null, productUrl);
               this.log("Position: " + this.position + " - Url: " + productUrl);
            }
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return (!this.currentDoc.select(".vtex-search-result-3-x-galleryItem").isEmpty()) && (this.arrayProducts.size() < this.totalProducts);
   }

   @Override
   protected void setTotalProducts() {
      this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, ".vtex-search-result-3-x-totalProducts--layout > span", false, 0);
      this.log("Total da busca: " + this.totalProducts);
   }

   private JSONArray scrapProductsArray(Document doc) {
      JSONObject dataJson = CrawlerUtils.selectJsonFromHtml(doc, ".vtex-store__template script[type=\"application/ld+json\"]", null, null, false, true);
      return JSONUtils.getJSONArrayValue(dataJson, "itemListElement");
   }
}
