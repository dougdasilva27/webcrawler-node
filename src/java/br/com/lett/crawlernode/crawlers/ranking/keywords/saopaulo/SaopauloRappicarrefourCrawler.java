package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class SaopauloRappicarrefourCrawler extends RappiCrawlerOld {

  public SaopauloRappicarrefourCrawler(Session session) {
    super(session, Arrays.asList("900020401"));
  }

}
