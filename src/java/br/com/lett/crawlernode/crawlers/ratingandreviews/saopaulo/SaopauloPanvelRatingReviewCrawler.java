package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
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

      ratingReviews.setTotalRating(crawlCommentsNumber(document));
      ratingReviews.setAverageOverallRating(crawlTotalRating(document));

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    }

    return ratingReviewsCollection;
  }

  private Double crawlTotalRating(Document document) {
    Integer stars = document.select(".item-info__rating i.active").size();
    return stars.doubleValue();
  }

  private Integer crawlCommentsNumber(Document doc) {
    Integer starNumber = 0;
    Element total = doc.select(".item-info__comments > span").first();

    if (total != null) {
      String text = total.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        starNumber = Integer.parseInt(text);
      }
    }

    return starNumber;
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
