package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class BrasilKoerichRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilKoerichRatingReviewCrawler(Session session) {
    super(session);
  }

  private JSONObject fetchApi() {
    JSONObject api = new JSONObject();

    String[] tokens = session.getOriginalURL().split("\\?")[0].split("/");
    String id = CommonMethods.getLast(tokens);
    String pathName = tokens[tokens.length - 2];

    String apiUrl =
        "https://www.koerich.com.br/ccstoreui/v1/pages/p/" + pathName + "/" + id + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=true";

    Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
    JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (response.has("data")) {
      api = response.getJSONObject("data");
    }

    return api;
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    JSONObject pageJson = fetchApi();

    if (pageJson.has("page")) {
      JSONObject page = pageJson.getJSONObject("page");

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      if (page.has("product")) {

        JSONObject json = page.getJSONObject("product");

        YourreviewsRatingCrawler yourReviews =
            new YourreviewsRatingCrawler(session, cookies, logger, "d3587a80-eb86-47a6-ac89-8de3f703770c", this.dataFetcher);
        String internalPid = crawlInternalPid(json);

        Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "d3587a80-eb86-47a6-ac89-8de3f703770c", this.dataFetcher);

        Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating);
        Double avgRating = getTotalAvgRatingFromYourViews(docRating);
        AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

        JSONArray arraySkus = json != null && json.has("childSKUs") ? json.getJSONArray("childSKUs") : new JSONArray();

        ratingReviews.setAdvancedRatingReview(advancedRatingReview);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        for (int i = 0; i < arraySkus.length(); i++) {
          JSONObject jsonSku = arraySkus.getJSONObject(i);
          String internalId = crawlInternalId(jsonSku);
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("repositoryId")) {
      internalId = json.get("repositoryId").toString();
    }

    return internalId;
  }

  private Double getTotalAvgRatingFromYourViews(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");

    if (rating != null) {
      avgRating = Double.parseDouble(rating.attr("content"));
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatingsFromYourViews(Document doc) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.selectFirst("meta[itemprop=ratingCount]");

    if (totalRatingElement != null) {
      totalRating = Integer.parseInt(totalRatingElement.attr("content"));
    }

    return totalRating;
  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("id")) {
      internalPid = json.get("id").toString();
    }

    return internalPid;
  }

}
