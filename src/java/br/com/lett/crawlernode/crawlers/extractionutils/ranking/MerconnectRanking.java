package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public abstract class MerconnectRanking extends CrawlerRankingKeywords {

   /**
    * Can be found in the request https://www.merconnect.com.br/mapp/v2/markets/by_neighborhood?cep=xxxxx
    * @return store id for cep
    */
   protected abstract String getStoreId();

   /**
    * Can be found in the request https://www.merconnect.com.br/oauth/token
    * @return client id
    */
   protected abstract String getClientId();

   /**
    * Can be found in the request https://www.merconnect.com.br/oauth/token
    * @return client secret
    */
   protected abstract String getClientSecret();

   protected abstract String getHomePage();

   protected MerconnectRanking(Session session) {
      super(session);
      pageSize = 25;
   }

   protected JSONObject fetch() {

      String url = "https://www.merconnect.com.br/mapp/v2/markets/" + getStoreId() + "/items/search?query=" + keywordEncoded.replace("+", "%20") + "&page=" + currentPage;

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Content-Type", "application/json");
      headers.put("Authorization", "Bearer " + fetchApiToken(headers));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return CrawlerUtils.stringToJson(response.getBody());
   }

   protected String fetchApiToken(Map<String, String> headers) {

      JSONObject payload = new JSONObject();
      payload.put("client_id", getClientId());
      payload.put("client_secret", getClientSecret());
      payload.put("grant_type", "client_credentials");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.merconnect.com.br/oauth/token")
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      JSONObject json = CrawlerUtils.stringToJson(dataFetcher.post(session, request).getBody());

      if (json != null && !json.isEmpty()) {
         return json.optString("access_token");
      }
      return null;
   }

   @Override
   public void extractProductsFromCurrentPage() {
      this.log("Página " + this.currentPage);

      JSONObject jsonResp = fetch();

      JSONArray array = jsonResp.optJSONArray("mixes");

      for (Object obj : array) {
         JSONObject product = (JSONObject) ((JSONObject) obj).optQuery("/items/0");
         String internalId = retrieveInternalId(product);
         String productUrl = retrieveUrl(product);
         saveDataProduct(internalId, null, productUrl);
         this.log("Position: " + this.position + " - InternalId: " + internalId + " - Url: " + productUrl);
      }
   }

   protected String retrieveUrl(JSONObject product) {
      try {
         return new URIBuilder(getHomePage()).setPath("produto").setPath(product.optString("id")).toString();
      } catch (URISyntaxException e) {
         return null;
      }
   }

   protected String retrieveInternalId(JSONObject product) {
      return product.optString("id");
   }

   @Override
   protected boolean hasNextPage() {
      return (arrayProducts.size() % pageSize - currentPage) < 0;
   }
}
