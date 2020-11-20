package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.GPACrawler;

public class RiodejaneiroExtraCrawler extends GPACrawler {

  private static final String CEP1 = "22640-901";

  public RiodejaneiroExtraCrawler(Session session) {
    super(session);
    this.cep = CEP1;
  }
}
