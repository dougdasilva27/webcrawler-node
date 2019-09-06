package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
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
    super.config.setFetcher(FetchMode.WEBDRIVER);
  }

  private static final String HOME_PAGE = "https://www.petz.com.br/";

  @Override
  protected Document fetch() {
    Document doc = new Document("");
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

    if (this.webdriver != null) {
      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

      Element script = doc.select("head script").last();
      Element robots = doc.select("meta[name=robots]").first();

      if (script != null && robots != null) {
        String eval = script.html().trim();

        if (!eval.isEmpty()) {
          Logging.printLogDebug(logger, session, "Execution of incapsula js script...");
          this.webdriver.executeJavascript(eval);
        }
      }

      String requestHash = FetchUtilities.generateRequestHash(session);
      this.webdriver.waitLoad(12000);

      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

      // saving request content result on Amazon
      S3Service.saveResponseContent(session, requestHash, doc.toString());
    }

    return doc;
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
          products.add(DynamicDataFetcher.fetchPage(webdriver, url, session));
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
