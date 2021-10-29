package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WalmartSuperCrawler extends CrawlerRankingKeywords {

   public WalmartSuperCrawler(Session session) {
      super(session);
   }

   String store_id = session.getOptions().optString("store_id");

   @Override
   public void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 20;

      this.log("Página " + this.currentPage);
      String url = "https://super.walmart.com.mx/api/wmx/search/?Ntt=" + this.keywordEncoded + "&Nrpp=20&No=" + this.arrayProducts.size()
         + "&storeId=" + store_id;
      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject search = fetchJSONApi(url);

      if (search.has("records") && search.getJSONArray("records").length() > 0) {
         JSONArray products = search.getJSONArray("records");

         if (this.totalProducts == 0) {
            setTotalProducts(search);
         }

         for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.getJSONObject(i);

            if (product.has("attributes")) {
               JSONObject attributes = product.getJSONObject("attributes");

               String productUrl = crawlProductUrl(attributes);
               String internalId = crawlInternalId(attributes);

               saveDataProduct(internalId, null, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + null + " - Url: " + productUrl);
            }

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

   protected void setTotalProducts(JSONObject search) {
      if (search.has("totalNumRecs")) {
         this.totalProducts = search.getInt("totalNumRecs");
         this.log("Total da busca: " + this.totalProducts);
      }
   }

   private String crawlInternalId(JSONObject product) {
      String internalId = null;

      if (product.has("record.id")) {
         JSONArray ids = product.getJSONArray("record.id");

         if (ids.length() > 0) {
            internalId = ids.get(0).toString();
         }
      }

      return internalId;
   }

   private String crawlProductUrl(JSONObject product) {
      String productUrl = null;

      if (product.has("product.seoURL")) {
         JSONArray urls = product.getJSONArray("product.seoURL");

         if (urls.length() > 0) {
            productUrl = "https://super.walmart.com.mx" + urls.get(0).toString().replace("[", "").replace("]", "");
         }
      }

      return productUrl;
   }

   private JSONObject fetchJSONApi(String url) {
      JSONObject api = new JSONObject();

      String referer = "https://super.walmart.com.mx/productos?Ntt=" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("Host", "super.walmart.com.mx");
      headers.put("Connection", "keep-alive");
      headers.put("x-dtreferer", referer);
      headers.put("Accept", "application/json");
      headers.put("Content-Type", "application/json");
      headers.put("Referer", referer);
      headers.put("Accept-Encoding", "");
      headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("Cache-Control", "no-cache");

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("contents")) {
         JSONArray contents = response.getJSONArray("contents");

         if (contents.length() > 0) {
            JSONObject content = contents.getJSONObject(0);

            if (content.has("mainArea")) {
               JSONArray mainArea = content.getJSONArray("mainArea");

               for (Object o : mainArea) {
                  JSONObject object = (JSONObject) o;

                  if (object.has("records")) {
                     api = object;
                     break;
                  }
               }
            }
         }
      }

      return api;
   }
}
