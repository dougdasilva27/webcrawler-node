package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import java.util.Arrays;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.RappiCrawlerOld;

public class SaopauloRappiCrawler extends RappiCrawlerOld {

  public SaopauloRappiCrawler(Session session) {
    super(session, Arrays.asList("700001704", "700001341"));
  }

}
