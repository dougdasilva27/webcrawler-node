package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilTelhanorteCrawler extends CrawlerRankingKeywords {

   public BrasilTelhanorteCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 20;

      this.log("Página " + this.currentPage);

      String url = "https://api.linximpulse.com/engage/search/v3/search?terms=" + this.keywordEncoded + "&origin=&apiKey=telhanorte&salesChannel=1&sortBy=relevance&showOnlyAvailable=true";
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("origin", "https://www.telhanorte.com.br");

      JSONObject json = new JSONObject(fetchGetFetcher(url, null, headers, null));

      if (json.has("products") && json.get("products") instanceof JSONArray) {
         JSONArray products = json.getJSONArray("products");

         if (products.length() >= 1) {
            if (this.totalProducts == 0) {
               setTotalBusca(json);
            }

            for (Object o : products) {
               JSONObject jsonProduct = (JSONObject) o;

               String internalPid = getInternalPid(jsonProduct);
               String productUrl = getUrl(jsonProduct);
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
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   private String getInternalPid(JSONObject product) {
      String pid = null;

      if (product.has("id")) {
         pid = product.get("id").toString();
      }

      return pid;
   }

   private String getUrl(JSONObject product) {
      String url = null;

      if (product.has("url")) {
         url = product.get("url").toString();

         if (!url.startsWith("http") && url.contains("telhanorte")) {
            url = "https:" + url;
         } else if (!url.contains("telhanorte")) {
            url = "https://www.telhanorte.com.br" + url;
         }
      }

      return url;
   }

   private void setTotalBusca(JSONObject json) {
      try {
         if (json.has("size")) {
            this.totalProducts = json.getInt("size");
         }
      } catch (Exception e) {
         this.logError(e.getMessage() + " Erro ao parsear jsonTotal");
      }
      this.log("Total da busca: " + this.totalProducts);
   }
}
