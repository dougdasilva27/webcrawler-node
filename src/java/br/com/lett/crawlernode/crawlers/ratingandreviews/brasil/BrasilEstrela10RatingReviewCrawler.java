package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import models.RatingsReviews;

public class BrasilEstrela10RatingReviewCrawler extends RatingReviewCrawler {

  public BrasilEstrela10RatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    TrustvoxRatingCrawler trustvox = new TrustvoxRatingCrawler(session, "30085", logger);
    JSONArray productsArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "var variants = ", ";", false, true);
    boolean hasVariations = doc.select(".sku-option").size() > 2;
    String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=ProductID]", "value");
    RatingsReviews ratingReviews = trustvox.extractRatingAndReviews(internalPid, doc, dataFetcher);

    // if product has variations, the first product is a product default, so is not crawled
    // then if is not, the last product is not crawled, because is a invalid product
    int indexStart = 0;
    int indexFinished = productsArray.length();

    if (hasVariations) {
      indexStart++;
    } else {
      indexFinished--;
    }

    for (int i = indexStart; i < indexFinished; i++) {
      JSONObject jsonSku = productsArray.getJSONObject(i);
      String internalId = JSONUtils.getStringValue(jsonSku, "productID");

      RatingsReviews clonedRatingReviews = ratingReviews.clone();
      clonedRatingReviews.setInternalId(internalId);
      ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
    }

    return ratingReviewsCollection;
  }
}
