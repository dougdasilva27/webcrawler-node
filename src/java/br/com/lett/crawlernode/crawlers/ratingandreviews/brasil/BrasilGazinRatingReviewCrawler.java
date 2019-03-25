package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilGazinRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilGazinRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);
      JSONObject ratingJson = jsonInfo.has("aggregateRating") ? jsonInfo.getJSONObject("aggregateRating") : new JSONObject();

      Integer totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(ratingJson, "reviewCount", 0);
      Double avgRating = CrawlerUtils.getDoubleValueFromJSON(ratingJson, "ratingValue", true, false);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0d);

      List<String> idList = scrapVariations(doc, jsonInfo);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".conteudopreco").isEmpty();
  }

  /**
   * Scrap variatons from html and returns a map with key internalId and value variationName
   * 
   * @param doc
   * @return
   */
  private List<String> scrapVariations(Document doc, JSONObject jsonInfo) {
    List<String> skus = new ArrayList<>();

    Elements variations = doc.select(".conteudopreco .ciq > div > div[id]");
    for (Element e : variations) {
      skus.add(e.id());
    }

    Elements variationsSpecial = doc.select(".conteudopreco .ciq select > option[class]");
    for (Element e : variationsSpecial) {
      skus.add(e.val());
    }

    if (skus.isEmpty()) {
      skus.add(jsonInfo.has("sku") ? jsonInfo.get("sku").toString() : null);
    }

    return skus;
  }
}
