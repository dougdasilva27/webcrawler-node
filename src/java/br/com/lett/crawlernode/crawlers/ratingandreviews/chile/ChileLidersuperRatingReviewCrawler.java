package br.com.lett.crawlernode.crawlers.ratingandreviews.chile;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class ChileLidersuperRatingReviewCrawler extends RatingReviewCrawler {

  public ChileLidersuperRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=productID]", true);
      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(doc));

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-info").isEmpty();
  }

  private Integer getTotalNumOfRatings(Document doc) {
    Integer numberRating = 0;
    Element totalRating = doc.selectFirst(".bvseo-reviewCount");

    if (totalRating != null) {
      String text = totalRating.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        numberRating = MathUtils.parseInt(text);
      }
    }
    return numberRating;

  }

  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    Double tempAvg = CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, ".bvseo-ratingValue", true);
    if (tempAvg != null) {
      avgRating = tempAvg;
    }

    return avgRating;
  }
}
