package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class FortalezaCompremaxCrawler extends CrawlerRankingKeywords {
   public FortalezaCompremaxCrawler(Session session) {
      super(session);
      fetchMode = FetchMode.FETCHER;
   }

   public String getAuthorization() {
      String apiURL = "https://www.merconnect.com.br/oauth/token";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");

      JSONObject payload = new JSONObject();
      payload.put("client_id", "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff");
      payload.put("client_secret", "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c");
      payload.put("grant_type", "client_credentials");

      Request request = Request.RequestBuilder.create().setUrl(apiURL).setHeaders(headers).setPayload(payload.toString()).build();
      String response = this.dataFetcher.post(session, request).getBody();

      JSONObject reponseJson = CrawlerUtils.stringToJson(response);

      String acessToken = reponseJson.optString("access_token");
      String tokenType = reponseJson.optString("token_type");


      return tokenType + " " + acessToken;
   }

   private JSONArray getProductsList() {
      JSONArray productList = new JSONArray();

      String apiUrl = "https://www.merconnect.com.br/mapp/v2/markets/36/items/search?query="+this.keywordEncoded+"&page="+this.currentPage+"&";

      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", getAuthorization());

      Request request = Request.RequestBuilder.create().setUrl(apiUrl).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      JSONObject productInfo = JSONUtils.stringToJson(response);
      productList = JSONUtils.getJSONArrayValue(productInfo, "mixes");

      return productList;
   }


   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {
      // número de produtos por página do market
      this.pageSize = 25;

      JSONArray mixes = getProductsList();

      for (Object arrayOfArrays : mixes) {

         JSONArray items = JSONUtils.getJSONArrayValue((JSONObject) arrayOfArrays, "items");

         for (Object productJsonInfo : items) {

            JSONObject jsonInfo = (JSONObject) productJsonInfo;

            if (jsonInfo != null) {

               String internalId = jsonInfo.optString("id");

               String internalPid = jsonInfo.optString("mix_id");

               String category = jsonInfo.optString("section_id");

               String productUrl = "https://compremax.com.br" +
                  "/loja/" + 36 +
                  "/categoria/" + category +
                  "/produto/" + internalId;

               saveDataProduct(internalId, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " +
                  internalPid + " - Url: " + productUrl);
            }
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }
}
