package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;

public class FortalezaObomvizinhoCrawler extends CrawlerRankingKeywords {
   public FortalezaObomvizinhoCrawler(Session session) {
      super(session);
   }


   private static final String URL_PRODUCT_PAGE = "https://loja.obomvizinho.com.br?id=";
   private static final String CEP = "60840-285";


   protected JSONObject fetch() {
      JSONObject api = new JSONObject();

      Map<String, String> headers = new HashMap<>();
      headers.put("Auth-Token", "RUsycjRnU1BLTndkblIyTnF1T3FvMGlnUDJKVWx4Nk95eC9IL0RaMU80dz0tLVl3dlBqUjJnK1p2amdheW9WRVlWM0E9PQ");
      headers.put("Connection", "keep-alive");

      String url = "https://www.merconnect.com.br/api/v4/markets?cep=" + CEP + "&market_codename=pinheiro";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      String content = this.dataFetcher.get(session, request).getBody();

      api = CrawlerUtils.stringToJson(content);

      return api;
   }

   private String getMarketID(JSONObject apiResponse) {

      String marketId = null;

      JSONArray markets = JSONUtils.getJSONArrayValue(apiResponse, "markets");

      if (markets != null) {
         for (Object arr : markets) {

            JSONObject jsonM = (JSONObject) arr;

            marketId = jsonM.optString("id");

         }
      }

      return marketId;
   }



   @Override
   public void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 0;

      JSONObject fetch = (JSONObject) fetch();
      String marketId = getMarketID(fetch);

      String url = "https://www.merconnect.com.br/api/v2/markets/" + marketId + "/items/search?query=" + this.keywordEncoded;

      this.log("Link onde são feitos os crawlers: " + url);

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String content = this.dataFetcher.get(session, request).getBody();

      JSONObject json = CrawlerUtils.stringToJson(content);

      JSONArray mixes = JSONUtils.getJSONArrayValue(json, "mixes");

      for (Object arrayOfArrays : mixes) {

         if (arrayOfArrays != null) {
            JSONArray array = (JSONArray) arrayOfArrays;

            for (Object productJsonInfo : array) {

               JSONObject jsonInfo = (JSONObject) productJsonInfo;

               if (jsonInfo != null) {

                  String internalId = jsonInfo.optString("id");

                  String internalPid = jsonInfo.optString("mix_id");

                  String productUrl = URL_PRODUCT_PAGE + jsonInfo.optString("bar_code");

                  saveDataProduct(internalId, internalPid, productUrl);

                  this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " +
                     internalPid + " - Url: " + productUrl);
               }
            }

         } else {
            this.result = false;
            this.log("Keyword sem resultado!");
         }
      }
      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
   }
}
