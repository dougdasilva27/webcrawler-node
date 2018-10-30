package br.com.lett.crawlernode.crawlers.ratingandreviews.mexico;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 20/10/18
 * 
 * @author gabriel
 *
 */
public class MexicoHebRatingReviewCrawler extends RatingReviewCrawler {

  public MexicoHebRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalReviewCount(doc);
      Double avgRating = getAverageOverallRating(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      Map<String, JSONObject> productsMap = extractProductsJson(doc);

      if (productsMap.isEmpty()) {
        ratingReviews.setInternalId(crawlInternalPid(doc));
        ratingReviewsCollection.addRatingReviews(ratingReviews);
      } else {
        for (Entry<String, JSONObject> entry : productsMap.entrySet()) {
          RatingsReviews clonedRatingReviews = ratingReviews.clone();
          clonedRatingReviews.setInternalId(entry.getKey());
          ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;

  }

  /**
   * 
   * @param doc
   * @return
   */
  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=product]").isEmpty();
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element pid = doc.selectFirst("input[name=product]");
    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private Integer getTotalReviewCount(Document doc) {
    Integer totalReviewCount = 0;

    Element reviewCount = doc.selectFirst("span[itemprop=reviewCount]");
    if (reviewCount != null) {
      String text = reviewCount.ownText().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        totalReviewCount = Integer.parseInt(text);
      }
    }

    return totalReviewCount;
  }

  private Double getAverageOverallRating(Document doc) {
    Double avgOverallRating = 0d;

    Element ratingAvg = doc.selectFirst(".rating-box .rating[style]");
    if (ratingAvg != null) {
      String avgString = CommonMethods.getLast(ratingAvg.attr("style").split(":")).replaceAll("[^0-9.]", "");

      if (!avgString.isEmpty()) {
        avgOverallRating = (5d * (Double.parseDouble(avgString) / 100d));
      }
    }

    return avgOverallRating;
  }

  private Map<String, JSONObject> extractProductsJson(Document doc) {
    Map<String, JSONObject> productsMap = new HashMap<>();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "Product.Config(", ");", false, true);
    if (json.has("attributes")) {
      JSONObject attributes = json.getJSONObject("attributes");

      for (String key : attributes.keySet()) {
        JSONObject attribute = attributes.getJSONObject(key);

        if (attribute.has("options")) {
          JSONArray options = attribute.getJSONArray("options");

          for (Object o : options) {
            JSONObject product = (JSONObject) o;

            if (product.has("label") && product.has("products")) {

              JSONArray products = product.getJSONArray("products");

              for (Object obj : products) {
                String id = obj.toString();

                if (productsMap.containsKey(id)) {
                  JSONObject productInfo = productsMap.get(id);

                  String name = "";
                  if (productInfo.has("name")) {
                    name = productInfo.get("name").toString();
                  }

                  if (product.has("label")) {
                    productInfo.put("name", name + " " + product.get("label"));
                  }

                  if (!productInfo.has("price") && product.has("price") && !product.get("price").toString().equals("0")) {
                    productInfo.put("price", product.get("price"));
                  }

                  if (!productInfo.has("oldPrice") && product.has("oldPrice") && !product.get("oldPrice").toString().equals("0")) {
                    productInfo.put("oldPrice", product.get("oldPrice"));
                  }

                  productsMap.put(id, productInfo);
                } else {
                  JSONObject productInfo = new JSONObject();

                  if (product.has("label")) {
                    productInfo.put("name", product.get("label"));
                  }

                  if (product.has("price") && !product.get("price").toString().equals("0")) {
                    productInfo.put("price", product.get("price"));
                  }

                  if (product.has("oldPrice") && !product.get("oldPrice").toString().equals("0")) {
                    productInfo.put("oldPrice", product.get("oldPrice"));
                  }

                  productsMap.put(id, productInfo);
                }
              }
            }
          }
        }
      }
    }

    return productsMap;
  }
}
