package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.DrogariaMinasbrasilNetCrawler;

public class BrasilDrogarianetCrawler extends DrogariaMinasbrasilNetCrawler {

  public BrasilDrogarianetCrawler(Session session) {
    super(session);
    super.host = "www.drogarianet.com.br";
  }
}
