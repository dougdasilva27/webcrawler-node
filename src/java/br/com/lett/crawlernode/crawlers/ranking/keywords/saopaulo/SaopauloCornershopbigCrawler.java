package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.MexicoCornershopCrawlerRanking;

public class SaopauloCornershopbigCrawler extends MexicoCornershopCrawlerRanking {

  public SaopauloCornershopbigCrawler(Session session) {
    super(session);
  }

  @Override protected String getStoreId() {
    return "5870";
  }

  @Override protected String getProductStoreId() {
    return "1481";
  }
}
