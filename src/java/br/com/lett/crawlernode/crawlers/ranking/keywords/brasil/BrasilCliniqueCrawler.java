package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class BrasilCliniqueCrawler extends CrawlerRankingKeywords {

   private static String URL_NEXT_PAGE = "";

   public BrasilCliniqueCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 12;
      this.log("Página " + this.currentPage);
      String url = "";

      if (this.currentPage > 1) {
         url = "https://www.clinique.com.br/enrpc/JSONControllerServlet.do?M=host%3Alocalhost%7Cport%3A26480%7Crecs_per_page%3A12&" + URL_NEXT_PAGE;
      } else {
         url = "https://www.clinique.com.br/enrpc/JSONControllerServlet.do?M=host%3Alocalhost%7Cport%3A26480%7Crecs_per_page%3A12"
               + "&D=" + this.keywordEncoded + "&N=";
      }

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject json = fetchJSONObject(url);

      if (json != null && !json.isEmpty()) {
         URL_NEXT_PAGE = JSONUtils.getValueRecursive(json, "MetaInfo.Next Page Link", String.class, null);

         JSONArray products = json.optJSONArray("Records");

         if (!products.isEmpty()) {
            for (Object e : products) {
               JSONObject product = (JSONObject) e;

               String internalId = JSONUtils.getValueRecursive(product, "Properties.s_PRODUCT_ID", String.class);

               String urlProduct = "https://www.clinique.com.br" + JSONUtils.getValueRecursive(product, "Properties.p_url", String.class);

               saveDataProduct(internalId, null, urlProduct);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: null" + " - Url: " + urlProduct);
               if (this.arrayProducts.size() == productsLimit)
                  break;

            }
         } else {
            this.result = false;
            this.log("Keyword sem resultado!");
         }
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return URL_NEXT_PAGE != null;
   }

}
