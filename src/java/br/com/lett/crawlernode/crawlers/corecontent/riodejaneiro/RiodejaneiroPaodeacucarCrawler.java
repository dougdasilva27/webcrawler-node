package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class RiodejaneiroPaodeacucarCrawler extends GPACrawler {

  private static final String CEP1 = "22640-901";

  public RiodejaneiroPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
