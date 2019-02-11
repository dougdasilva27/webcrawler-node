package br.com.lett.crawlernode.crawlers.ranking.keywords.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.FalabellaCrawler;

public class ColombiaFalabellaCrawler extends FalabellaCrawler {

  public ColombiaFalabellaCrawler(Session session) {
    super(session);

    this.setHomePage("https://www.falabella.com.co/falabella-co/");
  }
}
