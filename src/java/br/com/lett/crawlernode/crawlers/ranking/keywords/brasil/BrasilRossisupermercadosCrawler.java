package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class BrasilRossisupermercadosCrawler extends CrawlerRankingKeywords {

   public BrasilRossisupermercadosCrawler(Session session) {
      super(session);
   }

   public String getToken() {
      String token = null;

      String url = "https://api.rossidelivery.com.br/v1/auth/loja/login";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("origin", "https://www.rossidelivery.com.br");

      JSONObject payload = new JSONObject()
         .put("domain", "rossidelivery.com.br")
         .put("username", "loja")
         .put("key", "df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload.toString()).setCookies(cookies).build();
      JSONObject tokenJson = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      token = JSONUtils.getStringValue(tokenJson, "data");

      return token;
   }

   public JSONObject crawlApi(String token) {

      String url = "https://api.rossidelivery.com.br/v1/loja/buscas/produtos/filial/1/centro_distribuicao/1/termo/" + this.keywordEncoded;

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", token);

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject produtoInfo = JSONUtils.getJSONValue(jsonObject, "data");

      return produtoInfo;
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 47;

      this.log("Página " + this.currentPage);


      String url = "https://www.rossidelivery.com.br/produtos/buscas?q=" + this.keywordEncoded + "&page=" + this.currentPage;

      this.log("Link onde são feitos os crawlers: " + url);

      this.currentDoc = fetchDocument(url);

      String token = getToken();
      JSONObject json = crawlApi(token);
      JSONArray produtosArray = JSONUtils.getJSONArrayValue(json, "produtos");

      if (produtosArray.length() >= 1) {

         for (Object e : produtosArray) {

            JSONObject product = (JSONObject) e;

            String internalPid = product.optString("id");

            String internalId = product.optString("produto_id");

            String urlProductIncomplete = product.optString("link");
            String urlHost = "www.rossidelivery.com.br/produtos/detalhe/" + internalId;

            String urlProduct = CrawlerUtils.completeUrl(urlProductIncomplete, "http://", urlHost);

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



}
