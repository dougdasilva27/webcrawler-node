package br.com.lett.crawlernode.crawlers.ranking.keywords.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class FortalezaPaodeacucarCrawler extends GPAKeywordsCrawler {

  private static final String CEP1 = "60150-160";

  public FortalezaPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
