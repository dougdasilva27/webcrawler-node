package br.com.lett.crawlernode.crawlers.ratingandreviews.ribeiraopreto;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 */
public class RibeiraopretoSavegnagoRatingReviewCrawler extends RatingReviewCrawler {

  public RibeiraopretoSavegnagoRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);
      Document docRating = crawlPageRatings(internalId);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
      Double avgRating = getTotalAvgRating(docRating);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element elementInternalId = doc.select(".productReference").first();
    if (elementInternalId != null) {
      internalId = elementInternalId.text().trim();
    }

    return internalId;
  }

  /**
   * Api Ratings Url: https://service.yourviews.com.br/review/GetReview? Ex payload:
   * storeKey=87b2aa32-fdcb-4f1d-a0b9-fd6748df725a&productStoreId=85286&extendedField=&callback=_jqjsp&_1516980244481=
   *
   * @param internalId
   * @return document
   */
  private Document crawlPageRatings(String internalId) {
    Document doc = new Document("");

    String url = "https://service.yourviews.com.br/review/GetReview?storeKey=d23c4a07-61d5-43d3-97da-32c0680a32b8" + "&productStoreId=" + internalId
        + "&callback=_jqjsp&_1535991546340=";

    String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (response != null && response.contains("({")) {
      int x = response.indexOf('(') + 1;
      int y = response.indexOf(");", x);

      JSONObject json = new JSONObject(response.substring(x, y));

      if (json.has("html")) {
        doc = Jsoup.parse(json.get("html").toString());
      }
    }

    CommonMethods.saveDataToAFile(doc, "/home/gabriel/htmls/SAVEGNAGO.html");

    return doc;
  }

  /**
   * Average is calculate
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.select("meta[itemprop=ratingValue]").first();

    if (rating != null) {
      avgRating = Double.parseDouble(rating.attr("content"));
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in rating page
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.select("strong[itemprop=ratingCount]").first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }

  private boolean isProductPage(Document document) {
    return document.select("#___rc-p-sku-ids").first() != null;
  }
}
