package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class FortalezaPaodeacucarCrawler extends GPACrawler {

  private static final String CEP1 = "60150-160";

  public FortalezaPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
