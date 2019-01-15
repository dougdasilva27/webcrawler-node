package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilNutrineRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilNutrineRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,
          ".halfRight > input#produtoId", "value");

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".floatLeft.ten-column script[type]",
          "produto=", ",\nurano", true, false);

      Integer totalNumOfEvaluations = getTotalNumOfEvaluations(doc, json, "quantidadeAvaliacoes");
      Double avgRating = getAvgRating(doc, json, "mediaAvaliacao");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);

      System.out.println("VALS: " + internalId + " " + totalNumOfEvaluations + " " + avgRating);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".produtoDetalhes") != null;
  }

  private Integer getTotalNumOfEvaluations(Document doc, JSONObject json, String key) {
    Integer resp = 0;

    if (!json.isNull(key)) {
      resp = CrawlerUtils.getFloatValueFromJSON(json, key).intValue();
    }

    return resp;
  }

  private Double getAvgRating(Document doc, JSONObject json, String key) {
    Double resp = 0.0;

    if (!json.isNull(key)) {
      resp = CrawlerUtils.getDoubleValueFromJSON(json, key);
    }

    return resp;
  }
}
