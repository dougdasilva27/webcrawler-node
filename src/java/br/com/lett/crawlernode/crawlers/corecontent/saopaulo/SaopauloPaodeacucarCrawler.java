package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class SaopauloPaodeacucarCrawler extends GPACrawler {

  private static final String CEP1 = "01007-040";

  public SaopauloPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
