package br.com.lett.crawlernode.crawlers.ratingandreviews.colombia;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 12/12/2018
 * 
 * @author João Pedro
 *
 */

public class ColombiaLarebajaRatingReviewCrawler extends RatingReviewCrawler{

  public ColombiaLarebajaRatingReviewCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(doc);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc, totalNumOfEvaluations);

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
  
  private String crawlInternalId(Document doc) {
    String internalId = null;
    Element serchedId = doc.selectFirst(".detPproduct input[data-producto]");

    if(serchedId != null) {
      internalId = serchedId.attr("data-producto").trim();      
    }
    
    return internalId;
  }
  
  private Integer getTotalNumOfRatings(Document doc) {
    Integer totalNumOfRatings = 0;
    Element element = doc.selectFirst(".content-resena p");
    String [] str  = element.text().split(" ");

    if(element != null) {
      totalNumOfRatings = Integer.parseInt(str[0]);      
    }
    
    return totalNumOfRatings;
  }
  
  private Double getTotalAvgRating(Document doc, Integer totalNumOfEvaluations) {
      Double avgRating = 0.0;
      Element rating = doc.selectFirst("div[data-score]");
      if(rating != null) {
        avgRating = Double.parseDouble(rating.attr("data-score").trim());
        
      }
      return avgRating;
  }

  
  private boolean isProductPage(Document doc) {    
    return !doc.select(".product_detail").isEmpty();
  }
}
