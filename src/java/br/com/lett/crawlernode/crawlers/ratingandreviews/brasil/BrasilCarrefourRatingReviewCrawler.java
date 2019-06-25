package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.RatingsReviews;

public class BrasilCarrefourRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilCarrefourRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL()));
  }

  private String fetchPage(String url) {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put("upgrade-insecure-requests", "1");
    headers.put("referer", "https://www.carrefour.com.br/");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
    return this.dataFetcher.get(session, request).getBody();
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = crawlRatingReviews(document);
      ratingReviews.setInternalId(crawlInternalId(session.getOriginalURL()));

      ratingReviewsCollection.addRatingReviews(ratingReviews);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(String url) {
    if ((url.contains("/p/")))
      return true;
    return false;
  }

  private String crawlInternalId(String url) {
    String internalPid = null;

    if (url.contains("?")) {
      url = url.split("\\?")[0];
    }

    if (url.contains("/p/")) {
      String[] tokens = url.split("p/");

      if (tokens.length > 1 && tokens[1].contains("/")) {
        internalPid = tokens[1].split("/")[0];
      } else if (tokens.length > 1) {
        internalPid = tokens[1];
      }
    }
    return internalPid;
  }

  private RatingsReviews crawlRatingReviews(Document document) {
    RatingsReviews ratingReviews = new RatingsReviews();

    Map<String, Integer> ratingDistribution = crawlRatingDistribution(document);
    AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValueWithRate(ratingDistribution);

    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    ratingReviews.setDate(session.getDate());
    ratingReviews.setTotalRating(computeTotalReviewsCount(ratingDistribution));
    ratingReviews.setAverageOverallRating(crawlAverageOverallRating(document));

    return ratingReviews;
  }

  private Integer computeTotalReviewsCount(Map<String, Integer> ratingDistribution) {
    Integer totalReviewsCount = 0;

    for (String rating : ratingDistribution.keySet()) {
      if (ratingDistribution.get(rating) != null)
        totalReviewsCount += ratingDistribution.get(rating);
    }

    return totalReviewsCount;
  }

  private Double crawlAverageOverallRating(Document document) {
    Double avgOverallRating = null;

    Element avgOverallRatingElement =
        document.select(".sust-review-container .block-review-pagination-bar div.block-rating div.rating.js-ratingCalc").first();
    if (avgOverallRatingElement != null) {
      String dataRatingText = avgOverallRatingElement.attr("data-rating").trim();
      try {
        JSONObject dataRating = new JSONObject(dataRatingText);
        if (dataRating.has("rating")) {
          avgOverallRating = dataRating.getDouble("rating");
        }
      } catch (JSONException e) {
        Logging.printLogWarn(logger, session, "Error converting String to JSONObject");
      }
    }

    return avgOverallRating;
  }

  private Map<String, Integer> crawlRatingDistribution(Document document) {
    Map<String, Integer> ratingDistributionMap = new HashMap<String, Integer>();

    Elements ratingLineElements = document.select("div.tab-review ul.block-list-starbar li");
    for (Element ratingLine : ratingLineElements) {
      Element ratingStarElement = ratingLine.select("div").first();
      Element ratingStarCount = ratingLine.select("div").last();

      if (ratingStarElement != null && ratingStarCount != null) {
        String ratingStarText = ratingStarElement.text();
        String ratingCountText = ratingStarCount.attr("data-star");

        List<String> parsedNumbers = MathUtils.parseNumbers(ratingStarText);
        if (parsedNumbers.size() > 0 && !ratingCountText.isEmpty()) {
          ratingDistributionMap.put(parsedNumbers.get(0), Integer.parseInt(ratingCountText));
        }
      }
    }

    return ratingDistributionMap;
  }

  public static AdvancedRatingReview getTotalStarsFromEachValueWithRate(Map<String, Integer> ratingDistribution) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    for (Map.Entry<String, Integer> entry : ratingDistribution.entrySet()) {

      if (entry.getKey().equals("1")) {
        star1 = entry.getValue();
      }

      if (entry.getKey().equals("2")) {
        star2 = entry.getValue();
      }

      if (entry.getKey().equals("3")) {
        star3 = entry.getValue();
      }

      if (entry.getKey().equals("4")) {
        star4 = entry.getValue();
      }

      if (entry.getKey().equals("5")) {
        star5 = entry.getValue();
      }

    }

    return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
  }

}
