package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class SaopauloRappipaguemenosCrawler extends RappiCrawlerOld {

  public SaopauloRappipaguemenosCrawler(Session session) {
    super(session, Arrays.asList("900021154"));
  }

}
