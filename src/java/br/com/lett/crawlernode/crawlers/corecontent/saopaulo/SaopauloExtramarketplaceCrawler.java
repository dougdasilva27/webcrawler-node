package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVACrawler;

public class SaopauloExtramarketplaceCrawler extends CNOVACrawler {

  private static final String MAIN_SELLER_NAME_LOWER = "extra";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "extra.com.br";
  private static final String HOST = "www.extra.com.br";

  public SaopauloExtramarketplaceCrawler(Session session) {
    super(session);
    super.mainSellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.mainSellerNameLower2 = MAIN_SELLER_NAME_LOWER_2;
    super.marketHost = HOST;
  }
}
