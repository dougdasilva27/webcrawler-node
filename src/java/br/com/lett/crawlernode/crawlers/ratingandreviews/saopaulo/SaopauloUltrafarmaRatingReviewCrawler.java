package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class SaopauloUltrafarmaRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloUltrafarmaRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = crawlRatingReviews(doc);
      ratingReviews.setInternalId(crawlInternalId(doc));

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".div_prod_qualidade > span") != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element id = doc.selectFirst(".div_prod_qualidade > span");
    if (id != null) {
      String text = id.ownText();

      if (text.contains(":")) {
        internalId = CommonMethods.getLast(text.split(":"));

        if (internalId.contains("-")) {
          internalId = internalId.split("-")[0].trim();
        }
      }
    }

    return internalId;
  }

  private RatingsReviews crawlRatingReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();

    ratingReviews.setDate(session.getDate());
    ratingReviews.setTotalRating(computeTotalReviewsCount(doc));
    ratingReviews.setTotalWrittenReviews(computeTotalWrittenReviewsCount(doc));
    ratingReviews.setAverageOverallRating(crawlAverageOverallRating(doc));

    return ratingReviews;
  }

  private Integer computeTotalReviewsCount(Document doc) {
    return doc.select(".cont-div-avalia .div_estrela_comentario").size();
  }

  private Integer computeTotalWrittenReviewsCount(Document doc) {
    return doc.select(".cont-div-avalia .txt-coment").size();
  }

  private Double crawlAverageOverallRating(Document document) {
    Double avgOverallRating = null;

    Element percentageElement = document.select("#avaliacao img").first();
    if (percentageElement != null) {
      try {
        String avgString = CommonMethods.getLast(percentageElement.attr("src").split("/")).replaceAll("[^0-9.]", "");

        if (!avgString.isEmpty()) {
          avgOverallRating = Double.parseDouble(avgString);
        }
      } catch (Exception e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return avgOverallRating;
  }
}
