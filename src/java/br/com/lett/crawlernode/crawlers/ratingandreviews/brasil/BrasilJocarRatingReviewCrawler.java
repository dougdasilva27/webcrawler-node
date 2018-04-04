package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilJocarRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilJocarRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(crawlInternalId(document));
      ratingReviews.setAverageOverallRating(crawlRatingAvg(document));

      Integer countRating = crawlRatingCount(document);
      ratingReviews.setTotalRating(countRating);
      ratingReviews.setTotalWrittenReviews(countRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.select("#lblCodProduto").first() != null;
  }


  private String crawlInternalId(Document document) {
    String internalId = null;
    Element id = document.select("#lblCodProduto").first();

    if (id != null) {
      internalId = id.ownText().trim();
    }

    return internalId;
  }

  private Integer crawlRatingCount(Document doc) {
    Integer countRating = 0;

    Element count = doc.select("[itemprop=reviewCount]").first();

    if (count != null) {
      String text = count.attr("content").replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        countRating = Integer.parseInt(text);
      }
    }

    return countRating;
  }

  private Double crawlRatingAvg(Document doc) {
    Double avgRating = 0d;

    Element avg = doc.select("[itemprop=ratingValue]").first();

    if (avg != null) {
      String text = avg.attr("content").replaceAll("[^0-9.]", "").trim();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

}
