package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offers;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SavegnagoCrawler extends VTEXOldScraper {
   private final String HOME_PAGE  = "https://www.savegnago.com.br/";
   private final String SELLER_NAME  = "Savegnago Supermercados";
   private final String CITY_CODE = getCityCode();
   private final String CEP = getCEP();

   protected abstract String getCEP();
   protected abstract String getCityCode();

   public SavegnagoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(SELLER_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      RatingsReviews reviews = new RatingsReviews();

      YourreviewsRatingCrawler yrRC = new YourreviewsRatingCrawler(session, cookies, logger,"d23c4a07-61d5-43d3-97da-32c0680a32b8",dataFetcher);
      Document docRating = yrRC.crawlPageRatingsFromYourViews(internalPid, "d23c4a07-61d5-43d3-97da-32c0680a32b8", dataFetcher);
      Integer totalNumOfEvaluations = yrRC.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yrRC.getTotalAvgRatingFromYourViews(docRating);

      AdvancedRatingReview adRating = yrRC.getTotalStarsFromEachValue(internalPid);

      reviews.setTotalRating(totalNumOfEvaluations);
      reviews.setAverageOverallRating(avgRating);
      reviews.setTotalWrittenReviews(totalNumOfEvaluations);
      reviews.setAdvancedRatingReview(adRating);
      reviews.setDate(session.getDate());

      return reviews;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + CITY_CODE);
   }

   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }

}
