package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.FalabellaCrawlerUtils;

public class PeruFalabellaCrawler extends FalabellaCrawlerUtils {

  private static final String HOME_PAGE = "https://www.falabella.com.pe/";
  private static final String IMAGE_URL_CITY = "FalabellaPE/";

  public PeruFalabellaCrawler(Session session) {
    super(session);

    super.setHomePage(HOME_PAGE);
    super.setImageUrl(IMAGE_URL_CITY);
    super.setCurrencyHasCents(true);
    super.setApiKey("t6cq31k112riuu8rgp51fq5al");

  }
}
