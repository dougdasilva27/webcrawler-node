package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class BrasilNutriiCrawler extends CrawlerRankingKeywords {

   public BrasilNutriiCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.log("Página " + this.currentPage);

      //site hasn't pagination
      String url = "https://www.nutrii.com.br/busca?q=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      JSONObject json = CrawlerUtils.selectJsonFromHtml(this.currentDoc, "#__NEXT_DATA__", null, null, false, false);

      JSONArray products = json != null ? JSONUtils.getValueRecursive(json, "props.pageProps.data", JSONArray.class) : null;

      if (products != null) {
         for (Object obj : products) {
            if (obj instanceof JSONObject) {
               JSONObject product = (JSONObject) obj;

               String internalId = JSONUtils.getStringValue(product, "sku");

               String productUrl = "https://www.nutrii.com.br" + JSONUtils.getStringValue(product, "url");

               saveDataProduct(internalId, internalId, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalId + " - Url: " + productUrl);
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

}
