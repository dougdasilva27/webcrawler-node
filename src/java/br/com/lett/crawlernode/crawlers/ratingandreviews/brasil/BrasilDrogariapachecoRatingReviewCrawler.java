package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

/**
 * Date: 14/12/16
 * 
 * @author gabriel
 *
 *         In time crawler was made, there was no rating on any product in this market
 *
 */
public class BrasilDrogariapachecoRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilDrogariapachecoRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

    if (skuJson.length() > 0) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      if (skuJson.has("productId")) {
        String internalPid = Integer.toString(skuJson.getInt("productId"));
        YourreviewsRatingCrawler yr =
            new YourreviewsRatingCrawler(session, cookies, logger, "87b2aa32-fdcb-4f1d-a0b9-fd6748df725a", this.dataFetcher);

        Document docRating = yr.crawlPageRatingsFromYourViews(internalPid, "87b2aa32-fdcb-4f1d-a0b9-fd6748df725a", this.dataFetcher);

        Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
        Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
        AdvancedRatingReview advancedRatingReview = yr.getTotalStarsFromEachValue(internalPid);

        ratingReviews.setAdvancedRatingReview(advancedRatingReview);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        List<String> idList = crawlIdList(skuJson);
        for (String internalId : idList) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(internalId);
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }

      }

    }

    return ratingReviewsCollection;

  }

  /**
   * Api Ratings Url: https://service.yourviews.com.br/review/GetReview? Ex payload:
   * storeKey=f36ed866-d7ee-41b9-ad61-2d4ad29da507&productStoreId=85286&extendedField=&callback=_jqjsp&_1516980244481=
   *
   * @param internalPid
   * @return document
   */
  private Document crawlPageRatings(String internalPid) {
    Document doc = new Document("");

    String url = "https://service.yourviews.com.br/review/GetReview?storeKey=87b2aa32-fdcb-4f1d-a0b9-fd6748df725a&" + "productStoreId=" + internalPid
        + "&extendedField=&callback=_jqjsp&_1559052851756=";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    String response = this.dataFetcher.get(session, request).getBody();

    if (response != null && response.contains("({")) {
      int x = response.indexOf('(') + 1;
      int y = response.indexOf(")", x);

      JSONObject json = new JSONObject(response.substring(x, y));

      if (json.has("html")) {
        doc = Jsoup.parse(json.get("html").toString());
      }
    }

    return doc;
  }

  /**
   * Average is calculate
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating, Integer totalRating) {
    Double avgRating = null;
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
    Integer totalRating = null;
    Element totalRatingElement = doc.select("strong[itemprop=ratingCount]").first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }


  private List<String> crawlIdList(JSONObject skuJson) {
    List<String> idList = new ArrayList<>();

    if (skuJson.has("skus")) {
      JSONArray skus = skuJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("sku")) {
          idList.add(Integer.toString(sku.getInt("sku")));
        }
      }
    }

    return idList;
  }
}
