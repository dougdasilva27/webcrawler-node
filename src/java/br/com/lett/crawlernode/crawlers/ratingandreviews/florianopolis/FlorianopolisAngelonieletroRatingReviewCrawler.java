package br.com.lett.crawlernode.crawlers.ratingandreviews.florianopolis;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.AngelonieletroUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;


public class FlorianopolisAngelonieletroRatingReviewCrawler extends RatingReviewCrawler {

  public FlorianopolisAngelonieletroRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-productid]", "data-productid");
      String mainId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-sku]", "data-sku");

      Document voltageAPi = AngelonieletroUtils.fetchVoltageApi(internalPid, mainId, session, cookies, dataFetcher);
      Elements voltageVariation = voltageAPi.select("#formGroupVoltage input[name=voltagem]");

      if (!voltageVariation.isEmpty()) {
        for (Element e : voltageVariation) {
          ratingReviewsCollection.addRatingReviews(crawlRating(AngelonieletroUtils.fetchSkuHtml(doc, e, mainId, session, cookies, dataFetcher)));
        }
      } else {
        ratingReviewsCollection.addRatingReviews(crawlRating(doc));
      }
    }

    return ratingReviewsCollection;
  }

  private RatingsReviews crawlRating(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    String internalId = AngelonieletroUtils.crawlInternalId(doc);
    Integer totalNumOfEvaluations = CrawlerUtils.scrapSimpleInteger(doc, ".avaliacoes > span", true);
    Double avgRating = CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, "#starsProductDescription > span", true);

    ratingReviews.setInternalId(internalId);
    ratingReviews.setTotalRating(totalNumOfEvaluations != null ? totalNumOfEvaluations : 0);
    ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations != null ? totalNumOfEvaluations : 0);
    ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0);

    return ratingReviews;
  }

  private boolean isProductPage(Document document) {
    return !document.select("#titulo").isEmpty();
  }
}
