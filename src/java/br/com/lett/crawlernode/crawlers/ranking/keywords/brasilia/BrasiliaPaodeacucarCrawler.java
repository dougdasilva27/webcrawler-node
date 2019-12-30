package br.com.lett.crawlernode.crawlers.ranking.keywords.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.models.GPAKeywordsCrawler;

public class BrasiliaPaodeacucarCrawler extends GPAKeywordsCrawler {

  private static final String CEP1 = "70330-500";

  public BrasiliaPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
