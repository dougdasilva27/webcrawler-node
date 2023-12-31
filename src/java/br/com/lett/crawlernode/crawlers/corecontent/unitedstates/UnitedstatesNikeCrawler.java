package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.NikeCrawler;

public class UnitedstatesNikeCrawler extends NikeCrawler {

  protected static final String HOME_PAGE = "https://www.nike.com";
  protected static final String COUNTRY_URL = "/us/en_us/";

  public UnitedstatesNikeCrawler(Session session) {
    super(session);
    NikeCrawler.HOME_PAGE = HOME_PAGE;
    NikeCrawler.COUNTRY_URL = COUNTRY_URL;
  }
}
