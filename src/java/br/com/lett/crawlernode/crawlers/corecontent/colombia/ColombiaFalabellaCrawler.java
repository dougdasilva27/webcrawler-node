package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawlerUtils;

public class ColombiaFalabellaCrawler extends FalabellaCrawlerUtils {
  public ColombiaFalabellaCrawler(Session session) {
    super(session);

    super.setHomePage("https://www.falabella.com.co/");
    super.setImageUrl("FalabellaCO/");
    super.setCurrencyHasCents(false);
    super.setApiKey("oub11ocqqjr1ukjf43c9dukc9");
  }
}
