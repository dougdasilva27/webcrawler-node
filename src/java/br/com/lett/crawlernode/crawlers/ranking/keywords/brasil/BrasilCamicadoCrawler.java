package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilCamicadoCrawler extends CrawlerRankingKeywords {

   private static final String BASE_URL = "https://www.camicado.com.br";

   public BrasilCamicadoCrawler(Session session) {
      super(session);
   }

   @Override
   protected void extractProductsFromCurrentPage() {
      this.pageSize = 48;
      this.log("Página " + this.currentPage);

      JSONObject json = fetchApi();

      JSONArray products = json.optJSONArray("docs");

      if (products != null && !products.isEmpty()) {
         if (this.totalProducts == 0) {
            setTotalProducts(json);
         }

         for (Object obj : products) {
            JSONObject product = (JSONObject) obj;

            String internalId = null;
            String internalPid = product.optString("parent_product_id");
            String productUrl = BASE_URL + product.optString("linkId");

            saveDataProduct(internalId, internalPid, productUrl);

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

   private void setTotalProducts(JSONObject json) {
      this.totalProducts = json.optInt("numFound", 0);
      this.log("Total de produtos: " + this.totalProducts);
   }

   private JSONObject fetchApi() {
      JSONObject searchApi = new JSONObject();
      int start = 48 * (currentPage - 1);
      String url = "https://recs.richrelevance.com/rrserver/api/find/v1/077c13937e7836b7?" +
         "query=" + this.keywordEncoded +
         "&lang=pt" +
         "&rows=48" +
         "&placement=search_page.find" +
         "&start=" + start;
      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("Origin", "https://www.camicado.com.br");

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();
      JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (json.has("placements")) {
         JSONArray placements = json.getJSONArray("placements");

         if (placements.length() > 0) {
            searchApi = placements.getJSONObject(0);
         }
      }

      return searchApi;
   }
}
