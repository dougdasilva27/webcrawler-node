package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;

public class SaopauloPanvelRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloPanvelRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      JSONObject chaordic = CrawlerUtils.selectJsonFromHtml(document, "script", "window.chaordic_meta =", ";", false);
      JSONObject productJson = chaordic.has("product") ? chaordic.getJSONObject("product") : new JSONObject();

      ratingReviews.setInternalId(crawlInternalId(productJson));
      Integer commentsNumber = crawlCommentsNumber(document);

      ratingReviews.setTotalRating(commentsNumber);
      ratingReviews.setTotalWrittenReviews(commentsNumber);
      ratingReviews.setAverageOverallRating(crawlTotalRating(document, commentsNumber));

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }

    return ratingReviewsCollection;
  }

  private Double crawlTotalRating(Document document, Integer commentsNumber) {
    Double stars = 0d;

    if (commentsNumber > 0) {
      Double values = 0d;
      Elements comments = document.select(".box-comment .comment-title__rating");
      for (Element e : comments) {
        values += e.select("i.active").size();
      }

      stars = MathUtils.normalizeTwoDecimalPlaces(values / commentsNumber);
    }
    return stars;
  }

  private Integer crawlCommentsNumber(Document doc) {
    return doc.select(".box-comment").size();
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("id")) {
      internalId = product.get("id").toString();
    }

    return internalId;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".item-detalhe").first() != null;
  }

}
