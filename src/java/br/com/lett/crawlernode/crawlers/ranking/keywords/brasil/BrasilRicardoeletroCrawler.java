package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class BrasilRicardoeletroCrawler extends CrawlerRankingKeywords {

   public BrasilRicardoeletroCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.APACHE;
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String keyword = this.keywordWithoutAccents.replaceAll(" ", "+");

      String url = "https://www.ricardoeletro.com.br/Busca/Resultado/?p=" + this.currentPage + "&loja=&q=" + keyword;
      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      JSONArray jsonArray = CrawlerUtils.selectJsonArrayFromHtml(this.currentDoc, "script", "dataLayer = ", ";", false, true);
      JSONObject productsJSON = jsonArray.length() > 0 && jsonArray.get(0) instanceof JSONObject ? jsonArray.getJSONObject(0) : new JSONObject();
      JSONArray productsArray = JSONUtils.getJSONArrayValue(productsJSON, "searchProducts");
      Elements products = this.currentDoc.select("#products > div a");

      if (!products.isEmpty() && products.size() == productsArray.length()) {
         for (int index = 0; index < products.size(); index++) {
            Object obj = productsArray.get(index);
            JSONObject productJSON = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();

            String internalPid = scrapInternalPid(productJSON);
            String internalId = scrapInternalId(productJSON);
            String productUrl = CrawlerUtils.scrapUrl(products.get(index), null, "href", "https", "www.ricardoeletro.com.br");

            saveDataProduct(internalId, internalPid, productUrl);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
            if (this.arrayProducts.size() == productsLimit)
               break;

         }
      } else {
         setTotalProducts();
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
      if (!(hasNextPage()))
         setTotalProducts();
   }

   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select("li a.ultima:not(.inativo)").isEmpty();
   }

   private String scrapInternalId(JSONObject productJSON) {
      String internalId = null;

      if (productJSON.has("sku") && !productJSON.isNull("sku")) {
         internalId = productJSON.get("sku").toString();
      }

      return internalId;
   }

   private String scrapInternalPid(JSONObject productJSON) {
      String internalPid = null;

      if (productJSON.has("id") && !productJSON.isNull("id")) {
         internalPid = productJSON.get("id").toString();
      }

      return internalPid;
   }
}
