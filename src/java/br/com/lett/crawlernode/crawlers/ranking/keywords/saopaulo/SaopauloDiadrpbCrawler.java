package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloDiadrpbCrawler extends DiaCrawlerRanking {

  public SaopauloDiadrpbCrawler(Session session) {
    super(session);
  }

  private static final String CEP = "04530000";

  @Override
  public void processBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");
    String url = "https://www.dia.com.br/api/checkout/pub/postal-code/BRA/" + CEP;
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(url, null, "www.dia.com.br", "/", null, session, dataFetcher);
  }
}
