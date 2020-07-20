package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class PaguemenosCrawler extends VTEXNewScraper {

   public PaguemenosCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   private static final String HOME_PAGE = "https://www.paguemenos.com.br/";
   private static final List<String> MAIN_SELLERS = Arrays.asList("Pague Menos", "Farmácias Pague Menos");
   private RatingsReviews rating = new RatingsReviews();

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLERS;
   }

   @Override
   protected void processBeforeScrapVariations(Document doc, JSONObject productJson, String internalPid) {
      super.processBeforeScrapVariations(doc, productJson, internalPid);
      this.rating = scrapRating(internalPid);
   }

   @Override
   protected String scrapName(Document doc, JSONObject productJson, JSONObject jsonSku) {
      return (productJson.optString("brand") + " " + super.scrapName(doc, productJson, jsonSku)).trim();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return this.rating;
   }

   protected RatingsReviews scrapRating(String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Document docRating = crawlPageRatings(session.getOriginalURL(), internalPid);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
      Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);

      ratingReviews.setTotalRating(getTotalNumOfReviews(docRating));
      ratingReviews.setAverageOverallRating(avgRating);
      return ratingReviews;
   }

   /**
    * Page Ratings Url: http://www.paguemenos.com.br/userreview Ex payload:
    * productId=290971&productLinkId=ninho-fases-1-composto-lacteo Required headers to crawl this page
    * 
    * Ex: Média de avaliações: 5 votos
    *
    * 3 Votos nenhum voto 1 Voto 1 Voto nenhum voto
    * 
    * 
    * @param url
    * @param internalPid
    * @return document
    */
   private Document crawlPageRatings(String url, String internalPid) {
      Document doc = new Document(url);

      // Parameter in url for request POST ex: "led-32-ilo-hd-smart-d300032-" IN URL
      // "http://www.walmart.com.ar/led-32-ilo-hd-smart-d300032-/p"
      String[] tokens = url.split("/");
      String productLinkId = tokens[tokens.length - 2];

      String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");

      Request request = RequestBuilder.create().setUrl("https://www.paguemenos.com.br/userreview").setCookies(cookies).setHeaders(headers)
            .setPayload(payload).build();
      String response = this.dataFetcher.post(session, request).getBody();

      if (response != null) {
         doc = Jsoup.parse(response);
      }

      return doc;
   }

   /**
    * Average is calculate
    * 
    * @param document
    * @return
    */
   private Double getTotalAvgRating(Document docRating, Integer totalRating) {
      Double avgRating = 0d;
      Elements rating = docRating.select("ul.rating li");

      if (totalRating != null && totalRating > 0) {
         Double total = 0.0;

         for (Element e : rating) {
            Element star = e.select("strong.rating-demonstrativo").first();
            Element totalStar = e.select("> span:not([class])").first();

            if (totalStar != null) {
               String votes = totalStar.text().replaceAll("[^0-9]", "").trim();

               if (!votes.isEmpty()) {
                  Integer totalVotes = Integer.parseInt(votes);
                  if (star != null) {
                     if (star.hasClass("avaliacao50")) {
                        total += totalVotes * 5;
                     } else if (star.hasClass("avaliacao40")) {
                        total += totalVotes * 4;
                     } else if (star.hasClass("avaliacao30")) {
                        total += totalVotes * 3;
                     } else if (star.hasClass("avaliacao20")) {
                        total += totalVotes * 2;
                     } else if (star.hasClass("avaliacao10")) {
                        total += totalVotes * 1;
                     }
                  }
               }
            }
         }

         avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalRating);
      }

      return avgRating;
   }

   /**
    * Number of ratings appear in page rating
    * 
    * @param docRating
    * @return
    */
   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = 0;
      Element totalRatingElement = docRating.select(".media em > span").first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }

   /**
    * Number of ratings appear in page rating
    * 
    * @param docRating
    * @return
    */
   private Integer getTotalNumOfReviews(Document docRating) {
      return docRating.select(".resenhas .quem > li").size();
   }
}
