package br.com.lett.crawlernode.crawlers.ranking.keywords.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.FalabellaCrawler;

public class ArgentinaFalabellaCrawler extends FalabellaCrawler {

  public ArgentinaFalabellaCrawler(Session session) {
    super(session);

    this.setHomePage("https://www.falabella.com.ar/falabella-ar/");
  }
}
