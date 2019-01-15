package br.com.lett.crawlernode.crawlers.ratingandreviews.peru;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;

public class PeruMetroRatingReviewCrawler extends RatingReviewCrawler {

  public PeruMetroRatingReviewCrawler(Session session) {
    super(session);
  }

  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);
    System.err.println(skuJson);
    return ratingReviewsCollection;
  }
}
