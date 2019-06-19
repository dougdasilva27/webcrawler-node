package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.util.List;
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
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.AdvancedRatingReview;

public class YourreviewsRatingCrawler {

  private Session session;
  protected Logger logger;
  private List<Cookie> cookies;
  private Integer currentPage = 1;
  private String storeKey;
  private DataFetcher dataFetcher;

  public YourreviewsRatingCrawler(Session session, List<Cookie> cookies, Logger logger) {
    this.session = session;
    this.cookies = cookies;
    this.logger = logger;
  }

  public YourreviewsRatingCrawler(Session session, List<Cookie> cookies, Logger logger, String storeKey, DataFetcher dataFetcher) {
    this.session = session;
    this.cookies = cookies;
    this.logger = logger;
    this.storeKey = storeKey;
    this.dataFetcher = dataFetcher;
  }

  /**
   * Api Ratings Url: https://service.yourviews.com.br/review/GetReview? Ex payload:
   * storeKey=87b2aa32-fdcb-4f1d-a0b9-fd6748df725a&productStoreId=85286&extendedField=&callback=_jqjsp&_1516980244481=
   *
   * @param internalPid
   * @return document
   */
  public Document crawlPageRatingsFromYourViews(String internalPid, String storeKey, DataFetcher dataFetcher) {
    Document doc = new Document("");

    String url = "https://service.yourviews.com.br/review/GetReview?storeKey=" + storeKey + "&productStoreId=" + internalPid
        + "&extendedField=&callback=_jqjsp";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    String response = dataFetcher.get(session, request).getBody().trim();

    if (response.startsWith("<")) {
      doc = Jsoup.parse(response);
    } else if (response.contains("({")) {
      int x = response.indexOf("({") + 1;
      int y = response.lastIndexOf("})");

      String responseJson = response.substring(x, y + 1).trim();
      JSONObject json = CrawlerUtils.stringToJson(responseJson);

      if (json.has("html")) {
        doc = Jsoup.parse(json.get("html").toString());
      } else {
        doc = Jsoup.parse(response);
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

  public Document crawlAllPagesRatingsFromYourViews(String internalPid, String storeKey, DataFetcher dataFetcher, Integer currentPage) {
    Document doc = new Document("");

    String url = "https://service.yourviews.com.br/review/GetReview?storeKey=" + storeKey + "&productStoreId=" + internalPid + "&extFilters=&page="
        + currentPage + "&callback=_jqjsp&";

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    String response = dataFetcher.get(session, request).getBody().trim();

    if (response.startsWith("<")) {
      doc = Jsoup.parse(response);
    } else if (response.contains("({")) {
      int x = response.indexOf("({") + 1;
      int y = response.lastIndexOf("})");

      String responseJson = response.substring(x, y + 1).trim();
      JSONObject json = CrawlerUtils.stringToJson(responseJson);

      if (json.has("html")) {
        doc = Jsoup.parse(json.get("html").toString());
      } else {
        doc = Jsoup.parse(response);
      }
    }

    return doc;
  }

  public AdvancedRatingReview getTotalStarsFromEachValue(String internalPid) {
    Document docRating;

    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;

    do {
      docRating = crawlAllPagesRatingsFromYourViews(internalPid, storeKey, dataFetcher, this.currentPage);
      Elements reviews = docRating.select(".yv-col-md-8");
      for (Element element : reviews) {
        Elements stars = element.select(".fa-star");

        if (stars.size() == 1) {
          star1++;
        }

        if (stars.size() == 2) {
          star2++;
        }

        if (stars.size() == 3) {
          star3++;
        }

        if (stars.size() == 4) {
          star4++;
        }

        if (stars.size() == 5) {
          star5++;
        }

      }

    } while (hasNextPage(docRating));

    return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
  }

  public boolean hasNextPage(Document docRating) {
    boolean hasNextPage = false;

    Elements pages = docRating.select(".yv-paging:not(:last-child)");

    if (pages.size() > 0 && !pages.get(pages.size() - 1).text().trim().equals(this.currentPage.toString())) {
      hasNextPage = true;
      this.currentPage++;
    }

    return hasNextPage;
  }

}
