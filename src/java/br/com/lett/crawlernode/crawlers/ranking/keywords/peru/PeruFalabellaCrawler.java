package br.com.lett.crawlernode.crawlers.ranking.keywords.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.FalabellaCrawler;

public class PeruFalabellaCrawler extends FalabellaCrawler {

  public PeruFalabellaCrawler(Session session) {
    super(session);

    this.setHomePage("https://www.falabella.com.pe/falabella-pe/");
  }
}
