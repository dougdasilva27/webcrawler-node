package br.com.lett.crawlernode.crawlers.ratingandreviews.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.models.GPARatingCrawler;

public class BrasiliaPaodeacucarRatingReviewCrawler extends GPARatingCrawler {

  public static final String CEP1 = "70330-500";

  public BrasiliaPaodeacucarRatingReviewCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
