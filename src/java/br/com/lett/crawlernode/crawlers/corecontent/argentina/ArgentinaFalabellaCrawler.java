package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.FalabellaCrawlerUtils;

public class ArgentinaFalabellaCrawler extends FalabellaCrawlerUtils {
  public ArgentinaFalabellaCrawler(Session session) {
    super(session);

    super.setHomePage("https://www.falabella.com.ar/");
    super.setImageUrl("FalabellaAR/");
    super.setCurrencyHasCents(false);
    super.setApiKey("u5y9xkb4b1ly36cs5uvppykl0");
  }
}
