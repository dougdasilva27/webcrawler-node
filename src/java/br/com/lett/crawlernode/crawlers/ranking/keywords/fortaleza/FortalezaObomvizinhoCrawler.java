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

   String getStoreId() {
      return br.com.lett.crawlernode.crawlers.corecontent.fortaleza.FortalezaObomvizinhoCrawler.getStoreId();
   }

   public Map<String, String> getHeaders() {

      return br.com.lett.crawlernode.crawlers.corecontent.fortaleza.FortalezaObomvizinhoCrawler.getHeaders();
   }

   @Override
   public void extractProductsFromCurrentPage() {
      // número de produtos por página do market
      this.pageSize = 25;

      JSONObject json = fetch();

      JSONArray mixes = JSONUtils.getJSONArrayValue(json, "mixes");

      for (Object arrayOfArrays : mixes) {

         JSONArray items = JSONUtils.getJSONArrayValue((JSONObject) arrayOfArrays, "items");

         for (Object productJsonInfo : items) {

            JSONObject jsonInfo = (JSONObject) productJsonInfo;

            if (jsonInfo != null) {

               String internalId = jsonInfo.optString("id");

               String internalPid = jsonInfo.optString("mix_id");

               String category = jsonInfo.optString("section_id");

               String productUrl = "https://loja.obomvizinho.com.br" +
                  "/loja/" + getStoreId() +
                  "/categoria/" + category +
                  "/produto/" + internalId;

               saveDataProduct(internalId, internalPid, productUrl);

               this.log("Position: " + this.position + " - InternalId: " + internalId + " - name: " + jsonInfo.optString("short_description") + " - InternalPid: " +
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

   protected JSONObject fetch() {

      String url = "https://www.merconnect.com.br/mapp/v1/markets/" + getStoreId() + "/items/search" +
         "?query=" + this.keywordEncoded+
         "&page="  + this.currentPage;

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setHeaders(getHeaders())
         .build();

      String content = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);
   }
}
