package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;

public class SaopauloShoptimeCrawler extends B2WCrawler {

  private static final String HOME_PAGE = "https://www.shoptime.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "shoptime";

  public SaopauloShoptimeCrawler(Session session) {
    super(session);
    super.subSellers = new ArrayList<>();
    super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.homePage = HOME_PAGE;
  }
}
