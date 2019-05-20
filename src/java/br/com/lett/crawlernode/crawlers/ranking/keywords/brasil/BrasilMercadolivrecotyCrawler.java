package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.ranking.keywords.extractionutils.BrasilMercadolivreCrawler;

public class BrasilMercadolivrecotyCrawler extends BrasilMercadolivreCrawler {

  public BrasilMercadolivrecotyCrawler(Session session) {
    super(session);
    super.setStoreName("coty");
  }
}
