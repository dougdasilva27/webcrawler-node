package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.CNOVACrawler;

public class SaopauloCasasbahiaCrawler extends CNOVACrawler {

  private static final String MAIN_SELLER_NAME_LOWER = "casas bahia";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "casasbahia.com.br";
  private static final String HOST = "www.casasbahia.com.br";

  public SaopauloCasasbahiaCrawler(Session session) {
    super(session);
    super.mainSellerNameLower = MAIN_SELLER_NAME_LOWER;
    super.mainSellerNameLower2 = MAIN_SELLER_NAME_LOWER_2;
    super.marketHost = HOST;
  }
}
