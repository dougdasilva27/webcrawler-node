package br.com.lett.crawlernode.crawlers.ratingandreviews.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.models.GPARatingCrawler;

public class CuritibaPaodeacucarRatingReviewCrawler extends GPARatingCrawler {

  private static final String CEP1 = "80010-080";

  public CuritibaPaodeacucarRatingReviewCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
