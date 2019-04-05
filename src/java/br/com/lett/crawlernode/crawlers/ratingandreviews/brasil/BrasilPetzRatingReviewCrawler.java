package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.LettProxy;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 04/04/2018
 * 
 * @author gabriel
 *
 */
public class BrasilPetzRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilPetzRatingReviewCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.APACHE);
  }

  private static final String HOME_PAGE = "https://www.petz.com.br/";

  private String userAgent;
  private LettProxy proxyUsed;

  @Override
  public void handleCookiesBeforeFetch() {
    this.userAgent = FetchUtilities.randUserAgent();

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(HOME_PAGE).setCookies(cookies).setHeaders(headers).build();
    Response response = this.dataFetcher.get(session, request);

    this.proxyUsed = response.getProxyUsed();

    for (Cookie cookieResponse : response.getCookies()) {
      BasicClientCookie cookie = new BasicClientCookie(cookieResponse.getName(), cookieResponse.getValue());
      cookie.setDomain("www.petz.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
    }

  }

  @Override
  protected Document fetch() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.USER_AGENT, this.userAgent);

    Request request = RequestBuilder.create().setUrl(session.getOriginalURL()).setCookies(cookies).setHeaders(headers).setProxy(proxyUsed).build();
    return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      List<Document> documents = crawlIdList(doc);
      for (Document docProduct : documents) {
        RatingsReviews ratingReviews = new RatingsReviews();
        ratingReviews.setInternalId(crawlInternalId(docProduct));
        ratingReviews.setDate(session.getDate());
        Integer totalRating = getTotalNumOfRatings(docProduct);
        ratingReviews.setTotalRating(totalRating);
        ratingReviews.setTotalWrittenReviews(totalRating);
        ratingReviews.setAverageOverallRating(totalRating > 0 ? getTotalAvgRating(docProduct) : 0d);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private List<Document> crawlIdList(Document doc) {
    List<Document> products = new ArrayList<>();

    Elements variations = doc.select(".opt_radio_variacao[data-urlvariacao]");

    if (variations.size() > 1) {
      Logging.printLogInfo(logger, session, "Page with more than one product.");
      for (Element e : variations) {

        if (e.hasClass("active")) {
          products.add(doc);
        } else {
          String url = (HOME_PAGE + e.attr("data-urlvariacao")).replace("br//", "br/");
          Map<String, String> headers = new HashMap<>();
          headers.put(HttpHeaders.USER_AGENT, this.userAgent);

          Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setProxy(proxyUsed).setHeaders(headers).build();
          products.add(Jsoup.parse(this.dataFetcher.get(session, request).getBody()));
        }
      }
    } else {
      products.add(doc);
    }

    return products;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element sku = doc.select(".prod-info .reset-padding").first();
    if (sku != null) {
      internalId = sku.ownText().replace("\"", "").trim();
    }

    return internalId;
  }

  /**
   * Number of ratings appear in html element
   * 
   * @param doc
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    return doc.select(".ancora-depoimento").size();
  }

  /**
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = 0d;

    JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "\"extraInfo\":", ",", true, false);

    if (productJson.has("rating")) {
      avgRating = productJson.getDouble("rating");
    }

    return avgRating;
  }

  private boolean isProductPage(Document doc) {
    return doc.select(".prod-info").first() != null;
  }

}
