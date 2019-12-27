package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class SaopauloExtraCrawler extends GPAKeywordsCrawler {

  private static final String CEP1 = "01007-040";

  public SaopauloExtraCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }

}
