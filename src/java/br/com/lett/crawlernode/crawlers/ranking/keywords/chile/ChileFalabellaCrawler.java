package br.com.lett.crawlernode.crawlers.ranking.keywords.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.FalabellaCrawler;

public class ChileFalabellaCrawler extends FalabellaCrawler {

  public ChileFalabellaCrawler(Session session) {
    super(session);

    this.setHomePage("https://www.falabella.com/falabella-cl/");
  }
}
