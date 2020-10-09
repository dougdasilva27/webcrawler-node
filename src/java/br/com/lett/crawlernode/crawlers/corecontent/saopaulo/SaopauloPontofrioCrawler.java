package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVACrawler;
import br.com.lett.crawlernode.util.*;
import models.AdvancedRatingReview;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaopauloPontofrioCrawler extends CNOVACrawler {

   private static final String MAIN_SELLER_NAME_LOWER = "pontofrio";
   private static final String MAIN_SELLER_NAME_LOWER_2 = "pontofrio.com";
   private static final String HOST = "www.pontofrio.com.br";
   private static final String USER_AGENT = FetchUtilities.randUserAgent();
   public SaopauloPontofrioCrawler(Session session) {
      super(session);
      super.mainSellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.mainSellerNameLower2 = MAIN_SELLER_NAME_LOWER_2;
      super.marketHost = HOST;
   }


   private String crawlInternalPid(Document document) {
      String internalPid = null;
      Elements elementInternalId = document.select("script[type=text/javascript]");

      String idenfyId = "idProduct";

      for (Element e : elementInternalId) {
         String script = e.outerHtml();

         if (script.contains(idenfyId)) {
            script = script.replaceAll("\"", "");

            int x = script.indexOf(idenfyId);
            int y = script.indexOf(',', x + idenfyId.length());

            internalPid = script.substring(x + idenfyId.length(), y).replaceAll("[^0-9]", "").trim();
         }
      }
      return internalPid;
   }


   private JSONObject acessAPI(String internalPid, int totalNumOfEvaluations){

      String urlAPI = "https://avaliacoes.api-pontofrio.com.br/v1/api/produto/AvaliacoesPorProdutoPaginado?id="+internalPid+"&QuantidadeItensPagina="+totalNumOfEvaluations+"&Criterio=Data";

      Request request = Request.RequestBuilder.create().setUrl(urlAPI).build();
      JSONObject response = CrawlerUtils.stringToJson(new ApacheDataFetcher().get(session,request).getBody());

      return response != null? response: new JSONObject();
   }

   @Override
   protected AdvancedRatingReview scrapAdvancedRatingReview(Document doc, int totalNumOfEvaluations) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      String internalPid = crawlInternalPid(doc);
      JSONObject json = acessAPI(internalPid,totalNumOfEvaluations);
      JSONObject avaliacao = JSONUtils.getJSONValue(json,"avaliacao");
      JSONArray reviews = JSONUtils.getJSONArrayValue(avaliacao,"avaliacoes");

      if(!reviews.isEmpty()) {
         for(Object o: reviews ) {

            JSONObject review = (JSONObject) o;

            int star = review.optInt("notaArredondada");

            switch (star) {
               case 1:
                  star1 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 5:
                  star5 += 1;
                  break;
            }
         }
      }
      return new AdvancedRatingReview.Builder()
              .totalStar1(star1)
              .totalStar2(star2)
              .totalStar3(star3)
              .totalStar4(star4)
              .totalStar5(star5)
              .build();
   }
}
