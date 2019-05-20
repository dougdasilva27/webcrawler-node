package br.com.lett.crawlernode.crawlers.ratingandreviews.curitiba;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class CuritibaMuffatoRatingReviewCrawler extends RatingReviewCrawler {

  public CuritibaMuffatoRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

    if (skuJson.has("productId")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      List<String> idList = VTEXCrawlersUtils.crawlIdList(skuJson);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();

        JSONObject rating = crawlProductInformatioApi(internalId);

        Integer totalReviews = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
        Double avgRating = CrawlerUtils.getDoubleValueFromJSON(rating, "rate", true, false);

        clonedRatingReviews.setTotalRating(totalReviews);
        clonedRatingReviews.setTotalWrittenReviews(totalReviews);
        clonedRatingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private JSONObject crawlProductInformatioApi(String internalId) {
    JSONObject ratingJson = new JSONObject();

    String apiUrl = "https://awsapis3.netreviews.eu/product";
    String payload =
        "{\"query\":\"average\",\"products\":[\"" + internalId + "\"],\"idWebsite\":\"dd0aa2dc-6305-cd94-2106-9301054ace3c\",\"plateforme\":\"br\"}";
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=UTF-8");

    Request request =
        RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
    JSONObject response = CrawlerUtils.stringToJson(new FetcherDataFetcher().post(session, request).getBody());

    if (response.has(internalId)) {
      JSONArray rate = response.getJSONArray(internalId);

      if (rate.length() > 0) {
        ratingJson = rate.getJSONObject(0);
      }
    }

    return ratingJson;
  }
}
