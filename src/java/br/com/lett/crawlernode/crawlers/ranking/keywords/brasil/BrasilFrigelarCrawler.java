package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class BrasilFrigelarCrawler extends CrawlerRankingKeywords {

   public BrasilFrigelarCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetchJSONObject(String url) {
      return super.fetchJSONObject(url);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      //número de produtos por página do market
      this.pageSize = 12;

      String keyword = this.keywordWithoutAccents.replaceAll(" ", "%20");

      //monta a url com a keyword e a página
      String url = "https://www.frigelar.com.br/ccstoreui/v1/search?suppressResults=false&searchType=simple&No=" + (this.currentPage - 1) + 20 + "&Nrpp=20&Ntt=" + keyword;

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject jsonObject = fetchJSONObject(url);

      JSONObject resultsList = jsonObject != null ? jsonObject.optJSONObject("resultsList") : null;

      JSONArray productsArray = resultsList != null ? resultsList.optJSONArray("records") : null;

      if (productsArray != null && !productsArray.isEmpty()) {
         if (this.totalProducts == 0) setTotalProducts(resultsList);

         for (Object o : productsArray) {

            if (o instanceof JSONObject) {
               JSONObject products = (JSONObject) o;

               JSONObject attributes = JSONUtils.getValueRecursive(products, "records.0.attributes", JSONObject.class);

               if (attributes != null && !attributes.isEmpty()) {

                  String internalId = JSONUtils.getValueRecursive(attributes, "sku.displayName/0", "/", String.class, null);
                  String params = JSONUtils.getValueRecursive(attributes, "product.route/0", "/", String.class, null);
                  String urlProduct = CrawlerUtils.completeUrl(params, "https", "frigelar.com.br");

                  saveDataProduct(internalId, null, urlProduct);

                  this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
                  if (this.arrayProducts.size() == productsLimit) break;
               }
            }
         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }


   protected void setTotalProducts(JSONObject resultList) {
      this.totalProducts = resultList.optInt("totalNumRecs");

      this.log("Total da busca: " + this.totalProducts);
   }

}
