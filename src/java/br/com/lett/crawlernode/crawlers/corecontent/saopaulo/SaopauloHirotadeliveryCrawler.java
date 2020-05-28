package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SupermercadonowCrawler;

public class SaopauloHirotadeliveryCrawler extends SupermercadonowCrawler {

  public SaopauloHirotadeliveryCrawler(Session session) {
    super(session);
  }

  @Override protected String getLoadUrl() {
    return "hirota";
  }

  @Override protected String getSellerFullName() {
    return "hirota";
  }
}
