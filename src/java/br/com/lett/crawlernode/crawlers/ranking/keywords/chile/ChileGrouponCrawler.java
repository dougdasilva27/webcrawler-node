package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class ChileGrouponCrawler extends CrawlerRankingKeywords {

   public ChileGrouponCrawler(Session session) {
      super(session);
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      JSONObject productsApi = crawlInitalStateJson();
      JSONArray products = productsApi.has("deals") ? productsApi.getJSONArray("deals") : new JSONArray();

      if (productsApi.length() > 0) {
         if (this.totalProducts == 0) {
            setTotalProducts(productsApi);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            String internalPid = crawlInternalPid(product);
            String productUrl = crawlProductUrl(product);

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

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");

   }

   protected void setTotalProducts(JSONObject productsApi) {
      if (productsApi.has("pagination")) {
         JSONObject pagination = productsApi.getJSONObject("pagination");

         if (pagination.has("count")) {
            String text = pagination.get("count").toString().replaceAll("[^0-9]", "");

            if (!text.isEmpty()) {
               this.totalProducts = Integer.parseInt(text);
               this.log("Total da busca: " + this.totalProducts);
            }
         }
      }
   }

   private String crawlInternalPid(JSONObject product) {
      String internalPid = null;

      if (product.has("templateId")) {
         internalPid = product.get("templateId").toString();
      }

      return internalPid;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("dealUrl")) {
         productUrl = CrawlerUtils.completeUrl(product.get("dealUrl").toString(), "https:", "www.groupon.cl");
      }

      return productUrl;
   }

   private JSONObject crawlInitalStateJson() {
      JSONObject productsApi = new JSONObject();
      String element = "";
      String aux = "";

      String url = "https://www.groupon.cl/browse/santiago-centro?page=" + this.currentPage + "&query=" + this.keywordEncoded;
      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      Elements el = currentDoc.select("script");

      for (org.jsoup.nodes.Element elements : el) {
         aux = elements.toString();
         if (aux.contains("window.__APP_INITIAL_STATE__ = ")) {
            element = aux;
         }
      }
      int a = element.indexOf("=") + 3;
      int b = element.indexOf("\"<");
      element = element.substring(a, b);
      String decodedString = new String(Base64.getDecoder().decode(element.getBytes()));
      JSONObject initialState = CrawlerUtils.stringToJson(decodedString);

      if (initialState.has("dealSearchResult")) {
         JSONObject dealSearchResult = initialState.getJSONObject("dealSearchResult");

         if (dealSearchResult.has("resultsBrowse")) {
            productsApi = dealSearchResult.getJSONObject("resultsBrowse");
         }
      }

      return productsApi;
   }
}
