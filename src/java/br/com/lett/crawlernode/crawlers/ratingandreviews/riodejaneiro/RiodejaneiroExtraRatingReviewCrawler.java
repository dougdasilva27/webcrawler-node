package br.com.lett.crawlernode.crawlers.ratingandreviews.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ratingandreviews.models.GPARatingCrawler;

public class RiodejaneiroExtraRatingReviewCrawler extends GPARatingCrawler {

  public static final String CEP1 = "22640-901";

  public RiodejaneiroExtraRatingReviewCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
