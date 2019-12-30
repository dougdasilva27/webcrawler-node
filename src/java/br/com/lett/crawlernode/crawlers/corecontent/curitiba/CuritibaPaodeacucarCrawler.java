package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.GPACrawler;

public class CuritibaPaodeacucarCrawler extends GPACrawler {

  private static final String CEP1 = "80010-080";

  public CuritibaPaodeacucarCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
