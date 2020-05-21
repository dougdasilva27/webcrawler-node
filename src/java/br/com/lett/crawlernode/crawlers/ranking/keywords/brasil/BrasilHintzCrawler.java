package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilHintzCrawler extends CrawlerRankingKeywords {

   private static final String HOME_PAGE = "loja.hintz.ind.br";
   private static final String PROTOCOL = "https://";

   public BrasilHintzCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 21;

      this.log("Página " + this.currentPage);

      String url = PROTOCOL + HOME_PAGE + "/busca/?q=" + this.keywordEncoded + "&p=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      JSONArray arr = scrapArray();

      if (!arr.isEmpty()) {
         for (Object jsonOb : arr) {

            String internalPid = null;

            String incompleteUrl = JSONUtils.getStringValue((JSONObject) jsonOb, "url");

            String internalId = incompleteUrl.split("/")[2];
            String productUrl = CrawlerUtils.completeUrl(incompleteUrl, PROTOCOL, HOME_PAGE);

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
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

   private JSONArray scrapArray() {
      JSONArray json = new JSONArray();

      Element scripts = this.currentDoc.selectFirst("body script");

      String script = scripts.html().replaceAll(" ", "");

      if (script.contains("varprodutosData")) {

         String jsonString = CrawlerUtils.extractSpecificStringFromScript(script, "varprodutosData=", false, ";", false);
         json = CrawlerUtils.stringToJsonArray(jsonString);

      }
      return json;
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("#paginacao a[href]:not(first-child)").isEmpty();
   }

}
