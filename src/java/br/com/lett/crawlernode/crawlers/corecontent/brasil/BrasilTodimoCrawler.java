package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilTodimoCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.todimo.com.br/";
   private final String vtexSegment = this.session.getOptions().optString("vtex_segment");
   private final String zipcode = this.session.getOptions().optString("jb_zipcode");
   private String cookiesHeader = "";

   @Override
   protected List<String> getMainSellersNames() {
      return Collections.singletonList("Todimo Materiais para Construção SA");
   }

   public BrasilTodimoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      return CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#prd-description", "#prd-specifications"));
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie1 = new BasicClientCookie("vtex_segment", vtexSegment);
      cookie1.setDomain("www.todimo.com.br");
      cookie1.setPath("/");
      BasicClientCookie cookie2 = new BasicClientCookie("jb-zipCodeCurrent", zipcode);
      cookie2.setDomain("www.todimo.com.br");
      cookie2.setPath("/");

      this.cookies.add(cookie1);
      this.cookies.add(cookie2);

      cookiesHeader += "vtex_segment=" + vtexSegment;
      cookiesHeader += "; jb-zipCodeCurrent=" + zipcode;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      Map<String, String> headers = new HashMap<>();
      JSONObject productApi = new JSONObject();

      String url = homePage + "api/catalog_system/pub/products/search?fq=productId:" + internalPid + (parameters == null ? "" : parameters);

      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("cookie", cookiesHeader);
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.get(session, request);
      JSONArray array = CrawlerUtils.stringToJsonArray(response.getBody());

      if (!array.isEmpty()) {
         productApi = array.optJSONObject(0) == null ? new JSONObject() : array.optJSONObject(0);
      }

      return productApi;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return scrapRatingReviews(internalPid);
   }

   private RatingsReviews scrapRatingReviews(String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
      Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Document crawlApiRatings(String url, String internalPid) {

      String[] tokens = url.split("/");
      String productLinkId = tokens[tokens.length - 2];
      String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
      Request request = Request.RequestBuilder.create()
         .setUrl(homePage + "userreview")
         .setCookies(cookies)
         .setHeaders(headers)
         .setPayload(payload)
         .build();
      return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
   }

   private Double getTotalAvgRating(Document docRating, Integer totalRating) {

      Double avgRating = 0.0;
      Elements rating = docRating.select("ul.rating li");

      if (totalRating != null) {
         Double total = 0.0;

         for (Element e : rating) {
            Element star = e.select("strong").first();
            Element totalStar = e.select("li >  span:last-child").first();
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

   private AdvancedRatingReview scrapAdvancedRatingReview(Document docRating) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = docRating.select(".rating li");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst("Strong"); // <strong class="rating-demonstrativo avaliacao50"></strong>

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("class"); // rating-demonstrativo avaliacao50
            String sN = stringStarNumber.replaceAll("[^0-9]", "").trim(); // String 50
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0; // Integer 50

            Element elementVoteNumber = review.selectFirst("li > span:last-child"); // <span> 1 Voto</span>

            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim(); // 1 our ""
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0; // 1 our 0

               switch (numberOfStars) {
                  case 50:
                     star5 = numberOfVotes;
                     break;
                  case 44:
                     star4 = numberOfVotes;
                     break;
                  case 30:
                     star3 = numberOfVotes;
                     break;
                  case 20:
                     star2 = numberOfVotes;
                     break;
                  case 10:
                     star1 = numberOfVotes;
                     break;
                  default:
                     break;
               }
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

   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = null;
      Element totalRatingElement = docRating.selectFirst(".media em > span");

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }
}
