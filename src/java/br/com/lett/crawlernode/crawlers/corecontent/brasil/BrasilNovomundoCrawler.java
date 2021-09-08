package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class BrasilNovomundoCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "http://www.novomundo.com.br/";
   private static final List<String> MAIN_SELLER_NAME_LOWER = Collections.singletonList("novo mundo");

   public BrasilNovomundoCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public void handleCookiesBeforeFetch() {
      String vtex_segment = session.getOptions().optString("vtex_segment");
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtex_segment);
      this.cookies.add(cookie);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return MAIN_SELLER_NAME_LOWER;
   }

   @Override
   protected String scrapInternalpid(Document doc) {
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtex.events.addData(", ");", false, true);
      return jsonObject.optString("productId");

   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      RatingsReviews ratingReviews = new RatingsReviews();
      YourreviewsRatingCrawler rating = new YourreviewsRatingCrawler(session, cookies, logger, "4c93a458-0ff1-453e-b5b6-b361ad6aaeda", dataFetcher);

      Document docRating = rating.crawlPageRatingsFromYourViews(internalPid, "4c93a458-0ff1-453e-b5b6-b361ad6aaeda", dataFetcher);
      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(docRating, ".yv-row span strong", true, 0);
      Double avgRating = null;
      AdvancedRatingReview adRating = new AdvancedRatingReview();

      if (totalNumOfEvaluations > 0) {
          adRating = rating.getTotalStarsFromEachValue(internalPid);

          avgRating = getAvgRatingCalculate(adRating, totalNumOfEvaluations);


      }
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }

   private Double getAvgRatingCalculate(AdvancedRatingReview adRating, Integer totalNumOfEvaluations) {

      double totalStar1 = adRating.getTotalStar1();
      double totalStar2 = adRating.getTotalStar2() * 2;
      double totalStar3 = adRating.getTotalStar3() * 3;
      double totalStar4 = adRating.getTotalStar4() * 4;
      double totalStar5 = adRating.getTotalStar5() * 5;


      return (totalStar1 + totalStar2 + totalStar3 + totalStar4 + totalStar5) / totalNumOfEvaluations;

   }


}
