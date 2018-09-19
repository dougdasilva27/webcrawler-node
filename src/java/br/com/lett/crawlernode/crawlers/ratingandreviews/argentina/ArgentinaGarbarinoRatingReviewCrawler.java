package br.com.lett.crawlernode.crawlers.ratingandreviews.argentina;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class ArgentinaGarbarinoRatingReviewCrawler extends RatingReviewCrawler {
  
  public ArgentinaGarbarinoRatingReviewCrawler(Session session) {
    super(session);
  }
  
  
  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    
    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      
      String internalId = crawlInternalId(document);
      
      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document);
      
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return ratingReviewsCollection;
    
  }
  
  private boolean isProductPage(Document doc) {
    return doc.select(".title-product").first() != null;
  }
  
  private String crawlInternalId(Document doc) {
    String internalId = null;
    
    Element internalIdElement = doc.selectFirst(".gb--gray");
    if (internalIdElement != null) {
      internalId = internalIdElement.attr("data-product-id");
    }
    
    return internalId;
  }
  
  private Integer getTotalNumOfRatings(Document doc) {
    Integer numberRating = null;
    Element totalRating = doc.select(".gb-rating-legend-container > .gb-rating-legend").get(1);
    
    if (totalRating != null) {
      numberRating = MathUtils.parseInt(totalRating.text());
    }
    return numberRating;
    
  }
  
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;
    Element ratingElement = doc.selectFirst(".gb-product-reviews-rating > strong");
    
    if (ratingElement != null) {
      String valueRating = ratingElement.text().replaceAll("[^0-9.]", "");
      if (!valueRating.isEmpty()) {
        avgRating = Double.parseDouble(valueRating);
      }
    }
    
    return avgRating;
  }
}
