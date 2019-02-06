package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilLojastaqiRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilLojastaqiRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = scrapNumOfEval(doc);
      Double avgRating = scrapAvgRating(doc);
      String internalPid = scrapInternalPid(doc);

      Elements variations = doc.select(".atributos .item_atributo input");

      if (variations.size() > 0) {
        for (Element e : variations) {
          String internalId = e.hasAttr("value") ? e.attr("value") : null;

          ratingReviews.setInternalId(internalId);
          ratingReviews.setTotalRating(totalNumOfEvaluations);
          ratingReviews.setAverageOverallRating(avgRating);
          ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

          ratingReviewsCollection.addRatingReviews(ratingReviews);
        }

      } else {
        String internalId = scrapInternalId(doc, internalPid);

        ratingReviews.setInternalId(internalId);
        ratingReviews.setTotalRating(totalNumOfEvaluations);
        ratingReviews.setAverageOverallRating(avgRating);
        ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

        ratingReviewsCollection.addRatingReviews(ratingReviews);
      }
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".content_protudo") != null;
  }

  private String scrapInternalPid(Document document) {
    String internalPid = null;

    Element internalIdElement = document.selectFirst("meta[itemprop=sku]");

    if (internalIdElement != null) {
      internalPid = internalIdElement.attr("content");
    }

    return internalPid;
  }

  private String scrapInternalId(Document document, String pid) {
    String internalId = null;

    Element id = document.selectFirst("input#productSkuId_" + pid);

    if (id != null) {
      internalId = id.attr("value");
    } else {
      id = document.select("#skuSelected").first();

      if (id != null) {
        internalId = id.attr("value");
      }
    }


    return internalId;
  }

  private Integer scrapNumOfEval(Document doc) {
    Integer numOfEval = 0;
    Element e = doc.selectFirst(".avaie span:not([class])");

    if (e != null) {
      String aux = e.text().replaceAll("[^0-9]+", "");

      if (!aux.isEmpty()) {
        numOfEval = Integer.parseInt(aux);
      }
    }

    return numOfEval;
  }

  private Double scrapAvgRating(Document doc) {
    Double avgRating = 0.0;
    Elements elmnts = doc.select(".lista_avaliacao ul li span");

    Double partialSum = 0.0;
    int count = 0;

    for (int i = 0; i < elmnts.size(); i++) {
      Element e = elmnts.get(i);
      String aux = e.text().replaceAll("[^0-9]+", "");

      // evitando excecao no parseInt
      if (!aux.isEmpty()) {
        Integer auxI = Integer.parseInt(aux);
        count += auxI;
        partialSum += auxI * (elmnts.size() - i);
      }
    }

    // evitando divisao por 0
    if (count != 0) {
      avgRating = partialSum / count;
    }

    return avgRating;
  }
}
