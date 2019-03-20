package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 04/04/2018
 * 
 * @author gabriel
 *
 */
public class BrasilDafitiRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilDafitiRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      setRatingAndReviews(doc, ratingReviews);

      List<String> idList = crawlIdList(doc);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private void setRatingAndReviews(Document doc, RatingsReviews rating) {
    Element ratingText = doc.select(".ratings-reviews-component > a").first();

    if (ratingText != null) {
      String text = ratingText.attr("title");

      if (text.contains("nota") && text.contains("com") && text.contains("de")) {
        int x = text.indexOf("nota") + 4;
        int y = text.indexOf("de", x) + 2;
        int z = text.indexOf("com", y) + 3;

        String avg = text.substring(x, y).replaceAll("[^0-9.]", "").trim();
        String count = text.substring(z).replaceAll("[^0-9]", "").trim();

        if (!avg.isEmpty()) {
          rating.setAverageOverallRating(Double.parseDouble(avg));
        }

        if (!count.isEmpty()) {
          rating.setTotalRating(Integer.parseInt(count));
        }
      }
    }

    if (rating.getAverageOverallRating() == null) {
      rating.setAverageOverallRating(0d);
    }

    if (rating.getTotalReviews() == null) {
      rating.setTotalRating(0);
    }
  }

  private List<String> crawlIdList(Document doc) {
    List<String> internalIds = new ArrayList<>();

    Element elementSku = doc.select("#add-to-cart input[name=p]").first();

    if (elementSku != null) {
      String sku = elementSku.attr("value");

      String url = "https://www.dafiti.com.br/catalog/detailJson?sku=" + sku + "&_=1439492531368";
      JSONObject json = DataFetcherNO.fetchJSONObject(DataFetcherNO.GET_REQUEST, session, url, null, cookies);
      JSONArray sizes = json.has("sizes") ? json.getJSONArray("sizes") : new JSONArray();

      for (int i = 0; i < sizes.length(); i++) {
        internalIds.add(sizes.getJSONObject(i).getString("sku"));
      }
    }

    return internalIds;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-page").first() != null;
  }

}
