package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;


public class RiodejaneiroDrogariavenancioRatingReviewCrawler extends RatingReviewCrawler {
  private static final String HOME_PAGE = "https://www.drogariavenancio.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "venancio produtos farmaceuticos ltda";

  public RiodejaneiroDrogariavenancioRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        Integer totalNumOfEvaluations = 0;
        Double avgRating = scrapAvgRating(doc);

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private Double scrapAvgRating(Document doc) {
    Double num = 0.0;
    Element e = doc.selectFirst(".product__rating .rating-produto");

    if (e.hasClass("avaliacao1"))
      num = 1.0;
    else if (e.hasClass("avaliacao2"))
      num = 2.0;
    else if (e.hasClass("avaliacao3"))
      num = 3.0;
    else if (e.hasClass("avaliacao4"))
      num = 4.0;
    else if (e.hasClass("avaliacao5"))
      num = 5.0;

    return num;
  }
}
