package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import java.util.ArrayList;
import java.util.List;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 13/12/16
 * 
 * @author gabriel
 *
 */
public class SaopauloTendadriveRatingReviewCrawler extends RatingReviewCrawler {

  public SaopauloTendadriveRatingReviewCrawler(Session session) {
    super(session);
  }


  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    // shop id (AV guarapiranga)
    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=10");
    cookie.setDomain(".www.tendadrive.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(document, session);

      if (skuJson.has("productId")) {
        String internalPid = Integer.toString(skuJson.getInt("productId"));

        Document docRating = crawlPageRatings(internalPid);

        Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
        Double avgRating = getTotalAvgRating(docRating);

        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);

        List<String> idList = crawlIdList(skuJson);
        for (String internalId : idList) {
          RatingsReviews clonedRatingReviews = (RatingsReviews) ratingReviews.clone();
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

    String url = "https://service.yourviews.com.br/review/GetReview?storeKey=cbf269dc-e404-4b44-8090-1ad1dae7ea2e&" + "productStoreId=" + internalPid
        + "&extendedField=&callback=_jqjsp&_1519045771194=";

    String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (response != null && response.contains("({")) {
      int x = response.indexOf('(') + 1;
      int y = response.indexOf(");", x);

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
  private Double getTotalAvgRating(Document docRating) {
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

  private boolean isProductPage(Document document) {
    return document.select(".produto").first() != null;
  }

}
