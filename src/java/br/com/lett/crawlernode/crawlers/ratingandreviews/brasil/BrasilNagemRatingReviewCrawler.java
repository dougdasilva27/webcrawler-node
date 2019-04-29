package br.com.lett.crawlernode.crawlers.ratingandreviews.brasil;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.RatingReviewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;

public class BrasilNagemRatingReviewCrawler extends RatingReviewCrawler {
  public BrasilNagemRatingReviewCrawler(Session session) {
    super(session);
  }

  @Override
  protected RatingReviewsCollection extractRatingAndReviews(Document doc) throws Exception {
    RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,
          "#codigoproduto", "value");

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, 
          "#avaliacoes0 .container .row .avaliacoesProd .boxAvaliacao h3", true, 0);
      
      Double avgRating = getAvgRating(doc, 
          "#avaliacoes0 .container .row .avaliacoesProd .boxAvaliacao h3  i.fa-star");
      
      Integer totalWrittenReviews = getTotalWrittenReviews(doc,
          "#avaliacoes0 .container .row .avaliacaoComentario .avaliacaoComentarioEstrelas");

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);

      ratingReviewsCollection.addRatingReviews(ratingReviews);
    }

    return ratingReviewsCollection;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".bg_conteudo-produto") != null;
  }
  
  /**
   * Method to get rating by counting filled stars.
   * 
   * @param doc
   * @param selector
   * @return
   */
  private Double getAvgRating(Document doc, String selector) {
    Double resp = 0.0;
    
    Elements elements = doc.select(selector);
    resp += elements.size();
    
    return resp;
  }
  
  /**
   * Method to get written reviews by counting it.
   * 
   * @param doc
   * @param selector
   * @return
   */
  private Integer getTotalWrittenReviews(Document doc, String selector) {
    Integer resp = 0;
    
    Elements elements = doc.select(selector);
    resp += elements.size();
    
    return resp;
  }
}
