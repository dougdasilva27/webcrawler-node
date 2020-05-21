package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilKabumCrawler extends CrawlerRankingKeywords {

   public BrasilKabumCrawler(Session session) {
      super(session);
   }

   @Override
   protected void processBeforeFetch() {
      super.processBeforeFetch();
      this.cookies = CrawlerUtils.fetchCookiesFromAPage("https://www.kabum.com.br/", null, ".kabum.com.br", "/", cookies, session, null, dataFetcher);
   }

   private String baseUrl;
   private boolean isCategory = false;

   @Override
   protected void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);
      this.pageSize = 30;

      String url;
      if (!isCategory) {
         url = "https://www.kabum.com.br/cgi-local/site/listagem/listagem.cgi?string=" + this.keywordEncoded + "&pagina=" + this.currentPage;
      } else {
         url = this.baseUrl + "&pagina=" + this.currentPage;
      }

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      if (this.currentPage == 1) {
         this.baseUrl = CrawlerUtils.completeUrl(this.session.getRedirectedToURL(url), "https", "www.kabum.com.br");
         this.isCategory = this.baseUrl != null && !url.equalsIgnoreCase(baseUrl);
      }

      JSONArray arr = scrapArray();

      if (!arr.isEmpty()) {
         for (Object jsonObject : arr) {

            JSONObject json = (JSONObject) jsonObject;

            String incompleteUrl = JSONUtils.getStringValue(json, "link_descricao");
            String internalId = incompleteUrl.split("/")[2];
            String urlProduct = CrawlerUtils.completeUrl(incompleteUrl, "https://", "www.kabum.com.br");

            saveDataProduct(internalId, null, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + urlProduct);
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

      Elements scripts = this.currentDoc.select("body script[type=\"text/javascript\"]");
      String script = scripts.html().replaceAll(" ", "");

      if (script.contains("constlistagemDados")) {

         String jsonString = CrawlerUtils.extractSpecificStringFromScript(script, "constlistagemDados=", false, ";", false);
         json = CrawlerUtils.stringToJsonArray(jsonString);

      }
      return json;
   }

}
