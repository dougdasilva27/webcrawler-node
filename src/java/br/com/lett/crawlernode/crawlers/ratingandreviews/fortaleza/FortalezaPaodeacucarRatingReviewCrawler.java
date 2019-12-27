package br.com.lett.crawlernode.crawlers.ratingandreviews.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.models.GPARatingCrawler;

public class FortalezaPaodeacucarRatingReviewCrawler extends GPARatingCrawler {

  private static final String CEP1 = "60150-160";

  public FortalezaPaodeacucarRatingReviewCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
