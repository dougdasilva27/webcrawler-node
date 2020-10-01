package br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CarrefourCrawler extends CrawlerRankingKeywords {

   private static final String API_TOKEN = "55d1acadcd2e3ae60a6ea44fd238bffa5db5b72f06bfcfe6d8b46aba285f2cec";

   public CarrefourCrawler(Session session) {
      super(session);
   }

   private JSONObject fetchApi() throws UnsupportedEncodingException {

      String keyword = "{\"hideUnavailableItems\":false,\"skusFilter\":\"ALL_AVAILABLE\",\"simulationBehavior\":\"default\",\"installmentCriteria\":\"MAX_WITHOUT_INTEREST\",\"productOriginVtex\":false,\"map\":\"ft\",\"query\":\"" + this.keywordEncoded + "\",\"orderBy\":\"OrderByScoreDESC\",\"from\":0,\"to\":49,\"selectedFacets\":[{\"key\":\"ft\",\"value\":\"" + this.keywordEncoded + "\"}],\"fullText\":\"leite\",\"facetsBehavior\":\"dynamic\",\"withFacets\":false}";
      String encodedString = Base64.getEncoder().encodeToString(keyword.getBytes());

      String api = "https://mercado.carrefour.com.br/_v/segment/graphql/v1?workspace=master&maxAge=short&appsEtag=remove&domain=store&locale=pt-BR&operationName=productSearchV3&variables={}&extensions=";

      String query = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + API_TOKEN +"\",\"sender\":\"yourviews.yourviewsreviews@0.x\",\"provider\":\"yourviews.yourviewsreviews@0.x\"}," +
         "\"variables\":\"" + encodedString + "\"}";

      String encodedQuery = URLEncoder.encode(query, "UTF-8");

      this.log("Link onde são feitos os crawlers: " + api + encodedQuery);

      Request request = Request.RequestBuilder.create().setUrl(api+encodedQuery)
         .build();
      String response = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(response);
   }

   @Override
   protected void extractProductsFromCurrentPage() throws UnsupportedEncodingException {



   }
}
