package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.MexicoCornershopCrawlerRanking;

public class SaopauloCornershopbigpacaembuCrawler extends MexicoCornershopCrawlerRanking {

  public SaopauloCornershopbigpacaembuCrawler(Session session) {
    super(session);
  }

  @Override protected String getStoreId() {
    return "5870";
  }

  @Override protected String getProductStoreId() {
    return "1481";
  }
}
