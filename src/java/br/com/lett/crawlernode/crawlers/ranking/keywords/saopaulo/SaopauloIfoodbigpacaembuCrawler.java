package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.brasilia.BrasiliaCarrefourCrawler;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilIfood;

public class SaopauloIfoodbigpacaembuCrawler extends BrasilIfood {

  public static final String region = "sao-paulo-sp";
  public static final String store_name = "big-pacaembu---express-bom-retiro";
  
  public SaopauloIfoodbigpacaembuCrawler(Session session) {
    super(session);
  }

  @Override protected String getRegion() {
    return region;
  }

  @Override protected String getStore_name() {
    return store_name;
  }

}
