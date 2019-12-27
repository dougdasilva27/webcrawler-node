package br.com.lett.crawlernode.crawlers.ratingandreviews.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.models.GPARatingCrawler;

public class SaopauloPaodeacucarRatingReviewCrawler extends GPARatingCrawler {

  private static final String CEP1 = "01007-040";

  public SaopauloPaodeacucarRatingReviewCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
