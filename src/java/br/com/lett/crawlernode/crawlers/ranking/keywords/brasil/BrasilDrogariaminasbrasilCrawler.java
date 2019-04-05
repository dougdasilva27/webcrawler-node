package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.DrogariaMinasbrasilNetCrawler;

public class BrasilDrogariaminasbrasilCrawler extends DrogariaMinasbrasilNetCrawler {

  public BrasilDrogariaminasbrasilCrawler(Session session) {
    super(session);
    super.host = "www.drogariaminasbrasil.com.br";
  }
}
