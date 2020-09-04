package br.com.lett.crawlernode.crawlers.ranking.keywords.itauna;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;

import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ItaunaRenaemcasaCrawler extends CrawlerRankingKeywords {

   private static String apiUrl;
   private String token = "";
   private int totalPages;

   public ItaunaRenaemcasaCrawler(Session session){
      super(session);
   }

   private void changeUrl(){

      apiUrl = "https://api.itauna.renaemcasa.com.br/v1/loja/buscas/produtos/filial/1/centro_distribuicao/1/termo/" + this.keywordEncoded + "?page=" + this.currentPage;
   }

   private void getToken(){

      final String API_KEY = "df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469";
      final String API_DOMAIN = "itauna.renaemcasa.com.br";
      final String API_USERNAME = "loja";
      final String LOGIN_API = "https://api.itauna.renaemcasa.com.br/v1/auth/loja/login";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type:", "application/json");

      JSONObject jsonPayload = new JSONObject();

      jsonPayload.put("domain", API_DOMAIN);
      jsonPayload.put("key", API_KEY);
      jsonPayload.put("username", API_USERNAME);

      Request request = Request.RequestBuilder.create().setHeaders(headers).setPayload(jsonPayload.toString()).setUrl(LOGIN_API).build();

      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

      this.token = response.get("data").toString();
   }

   protected Object fetch() {

      Map<String, String> headers = new HashMap<>();
      String token = "Bearer " + this.token;
      headers.put("authorization", token);
      headers.put("content-type:", "application/json");

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(apiUrl).build();
      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() {

      this.pageSize = 52;

      this.log("Página " + this.currentPage);

      String url = "https://www.itauna.renaemcasa.com.br/produtos/buscas?q=" + this.keywordEncoded + "?page=" + this.currentPage;
      changeUrl();

      this.log("Link onde são feitos os crawlers: " + url);

      JSONObject json;
      JSONArray produtosArray;
      if(this.currentPage == 1){
         getToken();
         json = (JSONObject) fetch();
         JSONObject paginator = json.getJSONObject("paginator");
         totalPages = paginator.optInt("total_pages");
         JSONObject data = json.getJSONObject("data");
         produtosArray = JSONUtils.getJSONArrayValue(data, "produtos");
      } else{
         json = (JSONObject) fetch();
         JSONObject data = json.getJSONObject("data");
         produtosArray = JSONUtils.getJSONArrayValue(data, "produtos");
      }

      if (produtosArray.length() >= 1) {

         for (Object e : produtosArray) {

            JSONObject product = (JSONObject) e;

            String internalPid = product.optString("id");

            String internalId = product.optString("produto_id");

            String urlHost = "www.itauna.renaemcasa.com.br/produtos/detalhe/" + internalId;

            String urlProduct = CrawlerUtils.completeUrl("", "http://", urlHost);

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

      return this.currentPage < this.totalPages;
   }
}
