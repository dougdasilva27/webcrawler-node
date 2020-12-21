package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class VipcommerceRanking extends CrawlerRankingKeywords {

   private final String DOMAIN = getDomain();

   public VipcommerceRanking(Session session) {
      super(session);
   }

   protected abstract String getDomain();

   public String getToken() {
      String token = null;

      String url = "https://api."+ DOMAIN +"/v1/auth/loja/login";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("origin", "https://www."+ DOMAIN +"");

      JSONObject payload = new JSONObject()
         .put("domain", DOMAIN)
         .put("username", "loja")
         .put("key", "df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload.toString()).setCookies(cookies).build();
      JSONObject tokenJson = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      token = JSONUtils.getStringValue(tokenJson, "data");

      return token;
   }

   public JSONObject crawlApi(String token) {

      String url = "https://api."+ DOMAIN +"/v1/loja/buscas/produtos/filial/1/centro_distribuicao/1/termo/" + this.keywordEncoded + "?page=" + this.currentPage;

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", token);

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      return jsonObject;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 52;

      this.log("Página " + this.currentPage);


      String url = "https://www."+ DOMAIN +"/produtos/buscas?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      String token = getToken();
      JSONObject json = crawlApi(token);

      JSONObject produtoInfo = JSONUtils.getJSONValue(json, "data");
      JSONArray produtosArray = JSONUtils.getJSONArrayValue(produtoInfo, "produtos");

      if (produtosArray.length() >= 1) {

         for (Object e : produtosArray) {

            JSONObject product = (JSONObject) e;

            String internalPid = product.optString("id");

            String internalId = product.optString("produto_id");

            String urlProductIncomplete = product.optString("link");
            String urlHost = "www."+ DOMAIN +"/produtos/detalhe/" + internalId + "/";

            String urlProduct = "";

            if (urlProductIncomplete != null && urlHost != null) {
                urlProduct = "https://" + urlHost + urlProductIncomplete;
            }

            saveDataProduct(internalId, internalPid, urlProduct);

            this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + urlProduct);
            if (this.arrayProducts.size() == productsLimit) break;

         }
      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      String token = getToken();
      JSONObject json = crawlApi(token);
      JSONObject paginator = JSONUtils.getJSONValue(json, "paginator");

      return this.currentPage < paginator.optInt("total_pages");
   }
}
