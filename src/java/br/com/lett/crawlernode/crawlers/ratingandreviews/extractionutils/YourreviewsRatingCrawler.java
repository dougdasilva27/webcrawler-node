package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.List;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.session.Session;

public class YourreviewsRatingCrawler {


  private Session session;
  protected Logger logger;
  private List<Cookie> cookies;

  public YourreviewsRatingCrawler(Session session, List<Cookie> cookies, Logger logger) {
    this.session = session;
    this.cookies = cookies;
    this.logger = logger;
  }

  /**
   * Api Ratings Url: https://service.yourviews.com.br/review/GetReview? Ex payload:
   * storeKey=87b2aa32-fdcb-4f1d-a0b9-fd6748df725a&productStoreId=85286&extendedField=&callback=_jqjsp&_1516980244481=
   *
   * @param internalPid
   * @return document
   */
  public Document crawlPageRatingsFromYourViews(String internalPid, String storeKey) {
    Document doc = new Document("");

    String url = "https://service.yourviews.com.br/review/GetReview?storeKey=" + storeKey + "&productStoreId=" + internalPid
        + "&extendedField=&callback=_jqjsp";

    String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies).trim();

    if (response != null && response.contains("({")) {
      int x = response.indexOf("({") + 1;
      int y = response.lastIndexOf(");");

      String responseJson = response.substring(x, y).trim();

      if (responseJson.startsWith("{") && responseJson.endsWith("}")) {

        JSONObject json = new JSONObject(responseJson);

        if (json.has("html")) {
          doc = Jsoup.parse(json.get("html").toString());
        }
      }
    }

    return doc;
  }

  /**
   * Crawl rating avg from "your views" page, yourviews is found on this
   * function @crawlPageRatingsFromYourViews(String internalPid, String storeKey)
   * 
   * @param document
   * @return
   */
  public Double getTotalAvgRatingFromYourViews(Document docRating) {
    Double avgRating = 0d;
    Element rating = docRating.select("meta[itemprop=ratingValue]").first();

    if (rating != null) {
      avgRating = Double.parseDouble(rating.attr("content"));
    }

    return avgRating;
  }

  /**
   * Crawl rating count from "your views" page, yourviews is found on this
   * function @crawlPageRatingsFromYourViews(String internalPid, String storeKey)
   * 
   * @param docRating
   * @return
   */
  public Integer getTotalNumOfRatingsFromYourViews(Document doc) {
    Integer totalRating = 0;
    Element totalRatingElement = doc.select("strong[itemprop=ratingCount]").first();

    if (totalRatingElement != null) {
      String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

      if (!totalText.isEmpty()) {
        totalRating = Integer.parseInt(totalText);
      }
    }

    return totalRating;
  }
}
