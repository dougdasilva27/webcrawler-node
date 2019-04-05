package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class LeroymerlinRatingReviewCrawler extends RatingReviewCrawler {

  public LeroymerlinRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalId = crawlInternalId(document);
    JSONObject reviewSummary = new JSONObject();
    JSONObject primaryRating = new JSONObject();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String endpointRequest = assembleBazaarVoiceEndpointRequest(internalId, "caag5mZC6wgKSPPhld3GSUVaOqO46ZEpAemNYqZ38m7Yc");
      Request request = RequestBuilder.create().setUrl(endpointRequest).setCookies(cookies).build();
      JSONObject ratingReviewsEndpointResponse = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (ratingReviewsEndpointResponse.has("reviewSummary")) {
        reviewSummary = ratingReviewsEndpointResponse.getJSONObject("reviewSummary");

        if (reviewSummary.has("primaryRating")) {
          primaryRating = reviewSummary.getJSONObject("primaryRating");
        }
      }
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(getTotalRating(reviewSummary));
      ratingReviews.setAverageOverallRating(getAverageOverallRating(primaryRating));
      ratingReviews.setTotalWrittenReviews(getTotalRating(reviewSummary));

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }

  private Integer getTotalRating(JSONObject reviewSummary) {
    Integer total = 0;

    if (reviewSummary.has("numReviews") && reviewSummary.get("numReviews") instanceof Integer) {
      total = reviewSummary.getInt("numReviews");
    }

    return total;
  }

  private Double getAverageOverallRating(JSONObject primaryRating) {
    Double average = 0d;

    if (primaryRating.has("average") && primaryRating.get("average") instanceof Double) {
      average = primaryRating.getDouble("average");
    }

    return average;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-code").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.selectFirst(".product-code");
    if (internalIdElement != null) {
      String text = internalIdElement.text();

      if (text.contains(".")) {
        internalId = CommonMethods.getLast(text.split("\\.")).trim();
      } else if (text.contains("digo")) {
        internalId = CommonMethods.getLast(text.split("digo")).trim();
      } else {
        internalId = text.trim();
      }
    }

    return internalId;
  }

  // https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?PassKey=caag5mZC6wgKSPPhld3GSUVaOqO46ZEpAemNYqZ38m7Yc&productid=88100915
  // &contentType=reviews,questions&reviewDistribution=primaryRating,recommended&rev=0&contentlocale=pt_BR

  private String assembleBazaarVoiceEndpointRequest(String skuInternalId, String bazaarVoiceEnpointPassKey) {
    StringBuilder request = new StringBuilder();

    request.append("https://api.bazaarvoice.com/data/display/0.2alpha/product/summary?");
    request.append("&Passkey=" + bazaarVoiceEnpointPassKey);
    request.append("&productid=" + skuInternalId);
    request.append("&contentType=reviews,questions");
    request.append("&reviewDistribution=primaryRating,recommended");
    request.append("&rev=0");
    request.append("&contentlocale=pt_BR");

    return request.toString();
  }
}
