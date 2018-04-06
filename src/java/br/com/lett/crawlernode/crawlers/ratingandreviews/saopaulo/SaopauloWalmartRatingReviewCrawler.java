package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloWalmartRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloWalmartRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONArray products = crawlDataLayerJson(doc);

      if (products.length() > 0) {
        Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
        Integer numReviews = getTotalNumOfReviews(doc);
        Double avgRating = getTotalAvgRating(doc);

        if (totalNumOfEvaluations == 0 && numReviews > 0) {
          totalNumOfEvaluations = numReviews;
        }

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(numReviews);

        for (int i = 0; i < products.length(); i++) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(crawlInternalId(products.getJSONObject(i)));
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }

    }

    return ratingReviewsCollection;

  }


  private String crawlInternalId(JSONObject jsonProducts) {
    String productId = null;

    if (jsonProducts.has("skuId")) {
      productId = jsonProducts.get("skuId").toString();
    }

    return productId;
  }

  /**
   * Average is in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = null;
    Element rating = doc.select("#product-review .content-panel .section-title strong").first();

    if (rating != null) {
      String avgText = rating.text().replace(",", ".").trim();

      if (!avgText.isEmpty()) {
        avgRating = Double.parseDouble(avgText);
      }
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in html element
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document docRating) {
    Integer totalRating = 0;
    Element totalRatingElement = docRating.select(".star-rating-write-review").first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }

  /**
   * Number of ratings appear in html element
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfReviews(Document docRating) {
    Integer totalReviews = 0;
    Element totalRatingElement = docRating.select("#product-review h3").first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalReviews = Integer.parseInt(totalText);
      }
    }

    return totalReviews;
  }

  private boolean isProductPage(String url) {
    if (url.contains("walmart.com.br/produto/") || url.endsWith("/pr")) {
      return true;
    }
    return false;
  }

  /**
   * DataLayer that has all products in this page
   * 
   * "trees":[ { "productId":4413332, "standardSku":2901116, ... }, { "productId":4413333,
   * "standardSku":2901116, ... }
   * 
   * @param doc
   * @return
   */
  private JSONArray crawlDataLayerJson(Document doc) {
    // Pegar produtos dentro da url
    JSONObject dataLayer;
    JSONArray productsListInfo = new JSONArray();

    Elements scriptTags = doc.getElementsByTag("script");
    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var dataLayer = ") && tag.html().trim().contains("dataLayer.push(")) {

          dataLayer = new JSONObject(node.getWholeData().split(Pattern.quote("dataLayer.push("))[1]
              + node.getWholeData().split(Pattern.quote("dataLayer.push("))[1].split(Pattern.quote(");"))[0]);

          productsListInfo = dataLayer.getJSONArray("trees").getJSONObject(0).getJSONObject("skuTree").getJSONArray("options");

          if (productsListInfo.length() == 0) {
            productsListInfo
                .put(new JSONObject("{\"name\":\"\",\"skuId\":" + dataLayer.getJSONArray("trees").getJSONObject(0).get("standardSku") + "}"));
          }

        }
      }
    }

    return productsListInfo;
  }

}
