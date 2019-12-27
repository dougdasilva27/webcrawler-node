package br.com.lett.crawlernode.crawlers.corecontent.brasilia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class BrasiliaPaodeacucarCrawler extends GPACrawler {
  private static final String CEP1 = "70330-500";

  public BrasiliaPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
