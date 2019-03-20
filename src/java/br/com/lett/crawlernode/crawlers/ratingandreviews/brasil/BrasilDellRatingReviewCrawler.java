package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 25/07/17
 * 
 * @author gabriel
 *
 */
public class BrasilDellRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilDellRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject infoJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "Dell.Services.DataModel=", "};", true, false);

    if (infoJson.has("ProductMicroItems")) {
      JSONArray productsArray = infoJson.getJSONArray("ProductMicroItems");
      boolean hasVariations = productsArray.length() > 1;

      for (Object o : productsArray) {
        JSONObject productJson = (JSONObject) o;

        if (productJson.has("Mpn") && productJson.has("Url")) {
          String internalId = productJson.get("Mpn").toString();
          String newUrl = CrawlerUtils.completeUrl(productJson.get("Url").toString(), "https", "www.dell.com");
          Document newDoc = hasVariations ? DataFetcherNO.fetchDocument(DataFetcherNO.GET_REQUEST, session, newUrl, null, cookies) : doc;

          ratingReviewsCollection.addRatingReviews(crawlRating(internalId, newDoc));
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }


  private RatingsReviews crawlRating(String internalId, Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    Integer totalNumOfEvaluations = CrawlerUtils.scrapSimpleInteger(doc, "#productDetailsTopRatings span", true);
    Double avgRating = getTotalAvgRating(doc);

    ratingReviews.setInternalId(internalId);
    ratingReviews.setTotalRating(totalNumOfEvaluations != null ? totalNumOfEvaluations : 0);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations != null ? totalNumOfEvaluations : 0);
    ratingReviews.setAverageOverallRating(avgRating);

    return ratingReviews;
  }

  boolean isSpecialProduct(String url) {
    return url.contains("productdetail.aspx");
  }


  /**
   * Avg appear in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;
    Element rating = doc.selectFirst("#productDetailsTopRatings ratings");

    if (rating != null) {
      String text = rating.val().replaceAll("[^0-9.]", "").trim();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

}
