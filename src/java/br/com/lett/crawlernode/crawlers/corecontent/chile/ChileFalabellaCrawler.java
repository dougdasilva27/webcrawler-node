package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.FalabellaCrawlerUtils;

public class ChileFalabellaCrawler extends FalabellaCrawlerUtils {

  private static final String HOME_PAGE = "https://www.falabella.com/";
  private static final String IMAGE_URL_CITY = "Falabella/";

  public ChileFalabellaCrawler(Session session) {
    super(session);

    super.setHomePage(HOME_PAGE);
    super.setImageUrl(IMAGE_URL_CITY);
    super.setCurrencyHasCents(false);
  }
}
