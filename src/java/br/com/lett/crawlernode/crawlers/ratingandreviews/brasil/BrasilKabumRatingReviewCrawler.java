package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 20/07/17
 * 
 * @author gabriel
 *
 */
public class BrasilKabumRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilKabumRatingReviewCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "http://www.kabum.com.br";

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null, ".kabum.com.br", "/", cookies, session, null, dataFetcher);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(document);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;

  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    Element elementInternalID = doc.select(".boxs .links_det").first();

    if (elementInternalID != null) {
      String text = elementInternalID.ownText();
      internalId = text.substring(text.indexOf(':') + 1).trim();

      if (internalId.isEmpty()) {
        Element e = elementInternalID.select("span[itemprop=sku]").first();

        if (e != null) {
          internalId = e.ownText().trim();
        }
      }
    }

    return internalId;
  }

  /**
   * Avg appear in html element
   * 
   * @param document
   * @return
   */
  private Double getTotalAvgRating(Document doc) {
    Double avgRating = null;
    Element rating = doc.select(".avaliacao table tr td div.H-estrelas").first();

    if (rating != null) {
      String text = rating.attr("class").replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        avgRating = Double.parseDouble(text);
      }
    }

    return avgRating;
  }

  /**
   * Number of ratings appear in html element
   * 
   * @param doc
   * @return
   */
  private Integer getTotalNumOfRatings(Document doc) {
    Integer number = null;

    Element rating = doc.select(".avaliacao table tr td").first();

    if (rating != null) {
      String text = rating.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        number = Integer.parseInt(text);
      }
    }

    return number;
  }


  private boolean isProductPage(String url) {
    return url.startsWith("https://www.kabum.com.br/produto/") || url.startsWith("http://www.kabum.com.br/produto/") || url.contains("blackfriday");
  }

}
