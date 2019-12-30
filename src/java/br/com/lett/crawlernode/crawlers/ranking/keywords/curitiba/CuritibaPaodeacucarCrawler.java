package br.com.lett.crawlernode.crawlers.ranking.keywords.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class CuritibaPaodeacucarCrawler extends GPAKeywordsCrawler {

  private static final String CEP1 = "80010-080";

  public CuritibaPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
