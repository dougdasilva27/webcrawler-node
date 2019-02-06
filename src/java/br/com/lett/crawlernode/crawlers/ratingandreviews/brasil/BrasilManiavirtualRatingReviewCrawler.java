package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class BrasilManiavirtualRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilManiavirtualRatingReviewCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(document, "span[itemprop=\"reviewCount\"]");
      Double avgRating = getTotalAvgRatingFromYourViews(document, "span[itemprop=\"ratingValue\"]");

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }



  private Double getTotalAvgRatingFromYourViews(Document doc, String cssSelector) {
    Double avgRating = 0d;
    Element rating = doc.selectFirst(cssSelector);

    if (rating != null) {
      avgRating = MathUtils.parseDoubleWithDot(rating.text().trim());
    }

    return avgRating;
  }

  private Integer getTotalNumOfRatingsFromYourViews(Document doc, String cssSelector) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.selectFirst(cssSelector);

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-essential").first() != null;
  }

}
