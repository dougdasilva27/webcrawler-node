package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class RiodejaneiroExtraCrawler extends GPAKeywordsCrawler {

  private static final String CEP1 = "22640-901";

  public RiodejaneiroExtraCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
