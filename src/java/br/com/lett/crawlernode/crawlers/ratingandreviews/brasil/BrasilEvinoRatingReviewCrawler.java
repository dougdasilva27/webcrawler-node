package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class BrasilEvinoRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilEvinoRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    TrustvoxRatingCrawler trustvox = new TrustvoxRatingCrawler(session, "79779", logger);
    JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "var TC = ", null, false, false);
    JSONObject productBiggyJson = skuJson.has("productBiggyJson") ? new JSONObject(skuJson.get("productBiggyJson").toString()) : new JSONObject();
    String internalId = crawlInternalId(productBiggyJson);

    RatingsReviews ratingReviews = trustvox.extractRatingAndReviews(internalId, doc, dataFetcher);

    ratingReviews.setInternalId(internalId);
    ratingReviewsCollection.addRatingReviews(ratingReviews);

    return ratingReviewsCollection;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;
    JSONArray sku = product.getJSONArray("skus");

    for (Object object : sku) {
      JSONObject skuJson = (JSONObject) object;

      if (skuJson.has("sku")) {
        internalId = skuJson.get("sku").toString();
      }

    }

    return internalId;
  }

}
