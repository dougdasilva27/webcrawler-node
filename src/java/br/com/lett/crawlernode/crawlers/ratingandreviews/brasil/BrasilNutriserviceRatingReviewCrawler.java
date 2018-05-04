package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

/**
 * Date: 25/08/17
 * 
 * @author gabriel
 *
 */
public class BrasilNutriserviceRatingReviewCrawler extends RatingReviewCrawler {

  public BrasilNutriserviceRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document document) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
    if (isProductPage(document)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      List<String> idList = crawlIdList(document);
      for (String internalId : idList) {
        RatingsReviews clonedRatingReviews = ratingReviews.clone();
        clonedRatingReviews.setInternalId(internalId);
        ratingReviewsCollection.addRatingReviews(clonedRatingReviews);
      }

    }

    return ratingReviewsCollection;

  }

  private List<String> crawlIdList(Document doc) {
    List<String> internalIds = new ArrayList<>();
    Elements variations = doc.select(".variante select option:not([value=0])");

    if (!variations.isEmpty()) {
      for (Element e : variations) {
        internalIds.add(e.val());
      }
    } else {
      internalIds.add(crawlInternalId(doc));
    }

    return internalIds;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Elements scripts = doc.select("script[type=text/javascript]");

    for (Element e : scripts) {
      String script = e.outerHtml().replace("<script type=\"text/javascript\">", "").replaceAll(" ", "");

      if (script.contains("variante[0][0]=")) {
        String[] tokens = script.split(";");

        for (String token : tokens) {
          if (token.trim().contains("variante[0][0]=")) {
            internalId = token.split("=")[1].replace("'", "").trim();
            break;
          }
        }

        break;
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
    Double avgRating = 0d;
    Elements avg = doc.select(".avaliar .avaliacao_estrela img[src=\"http://www.nutriservice.com.br/Assets/Templates/1/imagens/estrela_on.png\"]");

    if (!avg.isEmpty()) {
      avgRating = ((Integer) avg.size()).doubleValue();
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
    Integer ratingNumber = 0;
    Element reviews = doc.select(".avaliar .votes").first();

    if (reviews != null) {
      String text = reviews.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        ratingNumber = Integer.parseInt(text);
      }
    }

    return ratingNumber;
  }


  private boolean isProductPage(Document doc) {
    return doc.select("#lblNome").first() != null;
  }

}
