package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;


public class VtexRatingCrawler {

  private Session session;
  private String homePage;
  protected Logger logger;
  private List<Cookie> cookies;

  public VtexRatingCrawler(Session session, String homePage, Logger logger, List<Cookie> cookies) {
    this.session = session;
    this.homePage = homePage;
    this.logger = logger;
    this.cookies = cookies;
  }

  /**
   * Extract rating info from trustVox API for vtex sites
   * 
   * @param document - html
   * @return
   */
  public RatingReviewsCollection extractRatingAndReviewsForVtex(Document document, DataFetcher dataFetcher) {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

    if (skuJson.has("productId")) {
      RatingsReviews ratingReviews = extractRatingAndReviews(skuJson.get("productId").toString(), document, dataFetcher);

      List<String> idList = VTEXCrawlersUtils.crawlIdList(skuJson);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }
    }

    return ratingReviewsCollection;
  }

  /**
   * Extract rating info from trustVox API
   * 
   * @param id - product Id
   * @param doc - html
   * @return
   */
  public RatingsReviews extractRatingAndReviews(String id, Document doc, DataFetcher dataFetcher) {
    RatingsReviews ratingReviews = new RatingsReviews();
    Document docRating = crawlApiRatings(session.getOriginalURL(), id, dataFetcher);

    Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
    Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);

    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setDate(session.getDate());

    return ratingReviews;
  }


  /**
   * Api Ratings Url: http://www.walmart.com.ar/userreview Ex payload:
   * productId=8213&productLinkId=home-theater-5-1-microlab-m-710u51 Required headers to crawl this
   * api
   * 
   * @param url
   * @param internalPid
   * @return document
   */
  private Document crawlApiRatings(String url, String internalPid, DataFetcher dataFetcher) {
    // Parameter in url for request POST ex: "led-32-ilo-hd-smart-d300032-" IN URL
    // "http://www.walmart.com.ar/led-32-ilo-hd-smart-d300032-/p"
    String[] tokens = url.split("/");
    String productLinkId = tokens[tokens.length - 2];

    String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");

    Request request =
        RequestBuilder.create().setUrl(this.homePage + "userreview").setCookies(cookies).setHeaders(headers).setPayload(payload).build();
    return Jsoup.parse(dataFetcher.post(session, request).getBody());
  }

  /**
   * Average is calculate
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document docRating, Integer totalRating) {
    Double avgRating = 0.0;
    Elements rating = docRating.select("ul.rating li");

    if (totalRating != null) {
      Double total = 0.0;

      for (Element e : rating) {
        Element star = e.selectFirst("strong.rating-demonstrativo");
        Element totalStar = e.selectFirst("> span:not([class])");

        if (totalStar != null) {
          String votes = totalStar.text().replaceAll("[^0-9]", "").trim();

          if (!votes.isEmpty()) {
            Integer totalVotes = Integer.parseInt(votes);
            if (star != null) {
              if (star.hasClass("avaliacao50")) {
                total += totalVotes * 5;
              } else if (star.hasClass("avaliacao40")) {
                total += totalVotes * 4;
              } else if (star.hasClass("avaliacao30")) {
                total += totalVotes * 3;
              } else if (star.hasClass("avaliacao20")) {
                total += totalVotes * 2;
              } else if (star.hasClass("avaliacao10")) {
                total += totalVotes * 1;
              }
            }
          }
        }
      }

      avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalRating);
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in api
   * 
   * @param docRating
   * @return
   */
  private Integer getTotalNumOfRatings(Document docRating) {
    Integer totalRating = null;
    Element totalRatingElement = docRating.selectFirst(".media em > span");

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }
}
