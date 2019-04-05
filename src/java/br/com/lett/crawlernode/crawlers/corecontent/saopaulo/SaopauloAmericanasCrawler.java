package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.B2WCrawler;

public class SaopauloAmericanasCrawler extends B2WCrawler {

  private static final String HOME_PAGE = "https://www.americanas.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";

  public SaopauloAmericanasCrawler(Session session) {
    super(session);
    super.subSellers = Arrays.asList("lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
    super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.homePage = HOME_PAGE;
  }
}
