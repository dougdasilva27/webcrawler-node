package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuperlagoaCrawler extends CrawlerRankingKeywords {

   private String storeId = session.getOptions().optString("store_id");


   public SuperlagoaCrawler(Session session) {
      super(session);
      super.fetchMode = FetchMode.FETCHER;
   }

   protected String getToken() {
      String url = "https://www.merconnect.com.br/oauth/token";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");
      String payload = "{\"client_id\":\"dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff\",\"client_secret\":\"27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c\",\"grant_type\":\"client_credentials\"}";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      Response content = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new JsoupDataFetcher(), new ApacheDataFetcher()), session, "post");

      JSONObject json = CrawlerUtils.stringToJson(content.getBody());

      return json.optString("access_token");
   }


   protected JSONObject fetch() {
      String url = "https://www.merconnect.com.br/mapp/v2/markets/" + storeId + "/items/search?query=" + this.keywordEncoded + "&page=" + this.currentPage + "&";

      String token = getToken();

      Map<String, String> headers = new HashMap<>();
      headers.put("Authorization", "Bearer " + token);

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      Response content = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new JsoupDataFetcher(), new ApacheDataFetcher()), session, "get");

      return CrawlerUtils.stringToJson(content.getBody());
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException, MalformedProductException {

      this.log("Página " + this.currentPage);

      JSONObject json = fetch();

      JSONArray jsonArray = JSONUtils.getValueRecursive(json, "mixes", JSONArray.class);

      if (jsonArray != null && !jsonArray.isEmpty()) {

         for (Object e : jsonArray) {

            JSONObject item = (JSONObject) e;
            JSONArray productsArray = JSONUtils.getValueRecursive(item, "items", JSONArray.class);

            for (Object o : productsArray) {
               JSONObject product = (JSONObject) o;

               if (product != null) {

                  String internalId = JSONUtils.getIntegerValueFromJSON(product, "id", 0).toString();
                  String internalPid = JSONUtils.getIntegerValueFromJSON(product, "id", 0).toString();

                  String urlProduct = urlProduct(product, internalPid);

                  String imgUrl = product.optString("image");
                  Double priceDouble = product.optDouble("price");
                  Integer priceInCents = (int) Math.round(100 * priceDouble);
                  String name = product.optString("description");
                  boolean isAvailable = product.optInt("stock") > 0;

                  RankingProduct productRanking = RankingProductBuilder.create()
                     .setUrl(urlProduct)
                     .setInternalPid(internalPid)
                     .setInternalId(internalId)
                     .setName(name)
                     .setImageUrl(imgUrl)
                     .setPriceInCents(priceInCents)
                     .setAvailability(isAvailable)
                     .build();

                  saveDataProduct(productRanking);

                  if (this.arrayProducts.size() == productsLimit) {
                     break;
                  }
               }
            }
         }

      } else {
         if (this.arrayProducts.isEmpty()) {

            this.result = false;
            this.log("Keyword sem resultado!");
         }

      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }

   @Override
   protected boolean hasNextPage() {
      return true;
   }

   private String urlProduct(JSONObject product, String internalPid) {
      String sessionId = JSONUtils.getIntegerValueFromJSON(product, "section_id", 0).toString();

      return "https://comprasonline.superlagoa.com.br/loja/" + storeId + "/categoria/" + sessionId + "/produto/" + internalPid;

   }
}
