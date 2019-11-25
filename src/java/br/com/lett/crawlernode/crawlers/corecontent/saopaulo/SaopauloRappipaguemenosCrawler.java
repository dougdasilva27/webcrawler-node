package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.RappiCrawler;

public class SaopauloRappipaguemenosCrawler extends RappiCrawler {

  public SaopauloRappipaguemenosCrawler(Session session) {
    super(session, "pague_menos");
  }

}
