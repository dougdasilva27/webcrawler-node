package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilEnutriRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilEnutriRatingReviewCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("loja", "base");
    cookie.setDomain(".www.enutri.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    String internalId = crawlInternalId(document);

    if (isProductPage(document)) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(getTotalRating(document));
      ratingReviews.setAverageOverallRating(getAverageOverallRating(document));
      ratingReviews.setTotalWrittenReviews(getTotalRating(document));

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }

  private Double getAverageOverallRating(Document document) {
    // x/100*5
    Element ratingElement = document.selectFirst(".rating");
    Double avg = 0d;

    if (ratingElement != null) {
      avg = (scrapDoubleFromAttr(ratingElement) / 100) * 5;
    }

    return avg;
  }

  private Double scrapDoubleFromAttr(Element element) {
    Double number = 0d;

    if (element.hasAttr("style")) {
      number = Double.parseDouble(element.attr("style").replaceAll("[^0-9]", ""));
    }

    return number;
  }

  private Integer getTotalRating(Document document) {
    return CrawlerUtils.scrapIntegerFromHtml(document, ".rating-links", false, 0);
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=product]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=product]").isEmpty();
  }


}
