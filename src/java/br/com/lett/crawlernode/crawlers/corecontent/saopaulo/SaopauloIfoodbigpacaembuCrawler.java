package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.BrasilIfood;

public class SaopauloIfoodbigpacaembuCrawler extends BrasilIfood {

  public static final String region = "sao-paulo-sp";
  public static final String store_name = "big-pacaembu---express-bom-retiro";
  public static final String seller_full_name = "Big Pacaembu";

  public SaopauloIfoodbigpacaembuCrawler(Session session) {
    super(session);
  }

  @Override protected String getRegion() {
    return region;
  }

  @Override protected String getStore_name() {
    return store_name;
  }

  @Override protected String getSellerFullName() {
    return seller_full_name;
  }


}

